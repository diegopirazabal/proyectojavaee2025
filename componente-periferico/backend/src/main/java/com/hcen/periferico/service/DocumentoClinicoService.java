package com.hcen.periferico.service;

import com.hcen.periferico.api.CentralAPIClient;
import com.hcen.periferico.dao.ClinicaDAO;
import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.dao.SolicitudAccesoDocumentoDAO;
import com.hcen.periferico.dto.documento_clinico_dto;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.entity.clinica;
import com.hcen.periferico.entity.documento_clinico;
import com.hcen.periferico.entity.profesional_salud;
import com.hcen.periferico.enums.TipoSincronizacion;
import com.hcen.periferico.messaging.DocumentoSincronizacionProducer;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.jms.JMSException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para gestión de documentos clínicos ambulatorios.
 * Implementa la lógica de negocio y validaciones.
 */
@Stateless
public class DocumentoClinicoService {

    private static final Logger LOGGER = Logger.getLogger(DocumentoClinicoService.class.getName());
    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    @EJB
    private DocumentoClinicoDAO documentoDAO;

    @EJB
    private ProfesionalSaludDAO profesionalDAO;

    @EJB
    private SincronizacionPendienteDAO sincronizacionDAO;

    @EJB
    private ClinicaDAO clinicaDAO;

    @EJB
    private SolicitudAccesoDocumentoDAO solicitudAccesoDAO;

    @EJB
    private CentralAPIClient centralAPIClient;

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    /**
     * Productor JMS para enviar documentos a la cola de sincronización
     */
    @EJB
    private DocumentoSincronizacionProducer sincronizacionProducer;

    /**
     * Crea un nuevo documento clínico
     */
    public documento_clinico crearDocumento(
            String usuarioSaludCedula,
            Integer profesionalCi,
            String codigoMotivoConsulta,
            String descripcionDiagnostico,
            LocalDate fechaInicioDiagnostico,
            String codigoEstadoProblema,
            String codigoGradoCerteza,
            LocalDate fechaProximaConsulta,
            String descripcionProximaConsulta,
            String referenciaAlta,
            UUID tenantId) {

        // Validaciones obligatorias
        validarCamposObligatorios(usuarioSaludCedula, profesionalCi, codigoMotivoConsulta,
                descripcionDiagnostico, fechaInicioDiagnostico, codigoGradoCerteza, tenantId);

        // Validar que el paciente exista
        UsuarioSalud paciente = em.find(UsuarioSalud.class, new UsuarioSalud.UsuarioSaludId(usuarioSaludCedula, tenantId));
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente con cédula " + usuarioSaludCedula + " no existe");
        }

        // Validar que el profesional exista y pertenezca al mismo tenant
        Optional<profesional_salud> profesionalOpt = profesionalDAO.findByCiAndTenantId(profesionalCi, tenantId);
        if (profesionalOpt.isEmpty()) {
            throw new IllegalArgumentException("El profesional con CI " + profesionalCi + " no existe en esta clínica");
        }
        profesional_salud profesional = profesionalOpt.get();

        // Validar que las codigueras existan
        validarCodigueras(codigoMotivoConsulta, codigoEstadoProblema, codigoGradoCerteza);

        // PRIMERO: Crear y persistir el documento localmente
        // NO asignamos el ID manualmente - Hibernate lo generará automáticamente con @GeneratedValue
        documento_clinico documento = new documento_clinico();
        documento.setTenantId(tenantId);
        documento.setUsuarioSaludCedula(usuarioSaludCedula);
        documento.setPaciente(paciente);
        documento.setProfesionalFirmante(profesional);
        documento.setProfesionalId(profesional.getId());
        documento.setFecCreacion(LocalDateTime.now());
        documento.setCodigoMotivoConsulta(codigoMotivoConsulta);
        documento.setDescripcionDiagnostico(descripcionDiagnostico.trim());
        documento.setFechaInicioDiagnostico(fechaInicioDiagnostico);
        documento.setCodigoEstadoProblema(codigoEstadoProblema);
        documento.setCodigoGradoCerteza(codigoGradoCerteza);
        documento.setFechaProximaConsulta(fechaProximaConsulta);
        documento.setDescripcionProximaConsulta(descripcionProximaConsulta != null ? descripcionProximaConsulta.trim() : null);
        documento.setReferenciaAlta(referenciaAlta != null ? referenciaAlta.trim() : null);
        documento.setHistClinicaId(null); // Se asignará después si la sincronización tiene éxito

        // Guardar documento localmente (Hibernate generará el UUID automáticamente)
        documento_clinico documentoGuardado = documentoDAO.save(documento);

        // Enviar a cola JMS para sincronización asíncrona con el central
        enviarDocumentoACola(documentoGuardado, tenantId);

        return documentoGuardado;
    }

    private void asegurarHistoriaClinicaLocal(UUID historiaId, String cedulaPaciente, UUID tenantId) {
        if (historiaId == null) {
            return;
        }
        Number result = (Number) em.createNativeQuery(
            "SELECT COUNT(1) FROM historia_clinica WHERE id = ?")
            .setParameter(1, historiaId)
            .getSingleResult();

        if (result.longValue() == 0L) {
            em.createNativeQuery(
                "INSERT INTO historia_clinica (id, usuario_cedula, tenant_id, fec_creacion) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING")
                .setParameter(1, historiaId)
                .setParameter(2, cedulaPaciente)
                .setParameter(3, tenantId)
                .setParameter(4, LocalDateTime.now())
                .executeUpdate();
        }
    }

    /**
     * Actualiza un documento clínico existente
     */
    public documento_clinico actualizarDocumento(
            UUID documentoId,
            String codigoMotivoConsulta,
            String descripcionDiagnostico,
            LocalDate fechaInicioDiagnostico,
            String codigoEstadoProblema,
            String codigoGradoCerteza,
            LocalDate fechaProximaConsulta,
            String descripcionProximaConsulta,
            String referenciaAlta,
            UUID tenantId) {

        // Validaciones obligatorias
        if (documentoId == null) {
            throw new IllegalArgumentException("El ID del documento es requerido");
        }
        validarCamposObligatorios(null, null, codigoMotivoConsulta,
                descripcionDiagnostico, fechaInicioDiagnostico, codigoGradoCerteza, tenantId);

        // Buscar documento existente
        Optional<documento_clinico> documentoOpt = documentoDAO.findByIdAndTenantId(documentoId, tenantId);
        if (documentoOpt.isEmpty()) {
            throw new IllegalArgumentException("El documento no existe o no tiene acceso");
        }

        // Validar codigueras
        validarCodigueras(codigoMotivoConsulta, codigoEstadoProblema, codigoGradoCerteza);

        // Actualizar campos
        documento_clinico documento = documentoOpt.get();
        documento.setCodigoMotivoConsulta(codigoMotivoConsulta);
        documento.setDescripcionDiagnostico(descripcionDiagnostico.trim());
        documento.setFechaInicioDiagnostico(fechaInicioDiagnostico);
        documento.setCodigoEstadoProblema(codigoEstadoProblema);
        documento.setCodigoGradoCerteza(codigoGradoCerteza);
        documento.setFechaProximaConsulta(fechaProximaConsulta);
        documento.setDescripcionProximaConsulta(descripcionProximaConsulta != null ? descripcionProximaConsulta.trim() : null);
        documento.setReferenciaAlta(referenciaAlta != null ? referenciaAlta.trim() : null);

        return documentoDAO.save(documento);
    }

    /**
     * Obtiene un documento por ID
     */
    public Optional<documento_clinico_dto> getDocumentoPorId(UUID documentoId, UUID tenantId) {
        Optional<documento_clinico> documentoOpt = documentoDAO.findByIdAndTenantId(documentoId, tenantId);
        return documentoOpt.map(this::convertirADTO);
    }

    /**
     * Lista documentos de un paciente de TODAS las clínicas
     * Muestra la historia clínica completa sin importar el tenant
     */
    public List<documento_clinico_dto> getDocumentosPorPaciente(String cedula, UUID tenantId) {
        List<documento_clinico> documentos = documentoDAO.findByPacienteAllTenants(cedula);
        return convertirListaADTOConCache(documentos);
    }

    /**
     * Lista documentos firmados por un profesional
     */
    public List<documento_clinico_dto> getDocumentosPorProfesional(Integer profesionalCi, UUID tenantId) {
        List<documento_clinico> documentos = documentoDAO.findByProfesional(profesionalCi, tenantId);
        return convertirListaADTOConCache(documentos);
    }

    /**
     * Lista documentos con paginación
     */
    public List<documento_clinico_dto> getDocumentosPaginados(UUID tenantId, int page, Integer size) {
        int resolvedSize = normalizePageSize(size);
        List<documento_clinico> documentos = documentoDAO.findAllByTenantIdPaginated(tenantId, page, resolvedSize);
        return convertirListaADTOConCache(documentos);
    }

    /**
     * Obtiene múltiples documentos por sus IDs (batch)
     * Útil para evitar N+1 queries cuando el backend central necesita varios documentos
     */
    public List<documento_clinico_dto> getDocumentosPorIds(List<UUID> documentoIds, UUID tenantId) {
        if (documentoIds == null || documentoIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<documento_clinico> documentos = documentoDAO.findByIdsAndTenantId(documentoIds, tenantId);
        return convertirListaADTOConCache(documentos);
    }

    /**
     * Obtiene múltiples documentos por sus IDs SIN filtrar por tenant (batch cross-tenant)
     * Usado por el componente central para recuperar documentos de un paciente
     * distribuidos en múltiples clínicas/tenants
     */
    public List<documento_clinico_dto> getDocumentosPorIds(List<UUID> documentoIds) {
        if (documentoIds == null || documentoIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<documento_clinico> documentos = documentoDAO.findByIds(documentoIds);
        return convertirListaADTOConCache(documentos);
    }

    /**
     * Cuenta documentos de una clínica
     */
    public long countDocumentos(UUID tenantId) {
        return documentoDAO.countByTenantId(tenantId);
    }

    /**
     * Cuenta documentos de un paciente de TODAS las clínicas
     */
    public long countDocumentosPorPaciente(String cedula, UUID tenantId) {
        return documentoDAO.countByPacienteAllTenants(cedula);
    }

    /**
     * Elimina un documento
     */
    public boolean eliminarDocumento(UUID documentoId, UUID tenantId) {
        return documentoDAO.deleteByIdAndTenantId(documentoId, tenantId);
    }

    // ============ MÉTODOS PRIVADOS ============

    /**
     * Convierte una entidad a DTO incluyendo resolución de codigueras
     */
    private documento_clinico_dto convertirADTO(documento_clinico documento) {
        documento_clinico_dto dto = new documento_clinico_dto();

        // IDs y fechas
        dto.setId(documento.getId().toString());
        dto.setTenantId(documento.getTenantId() != null ? documento.getTenantId().toString() : null);
        dto.setFecCreacion(documento.getFecCreacion());

        // Relaciones
        dto.setUsuarioSaludCedula(documento.getUsuarioSaludCedula());
        dto.setProfesionalCi(documento.getProfesionalFirmante().getCi());

        // Información del paciente
        if (documento.getPaciente() != null) {
            dto.setNombreCompletoPaciente(documento.getPaciente().getNombreCompleto());
        }

        // Información del profesional
        profesional_salud profesional = documento.getProfesionalFirmante();
        dto.setNombreCompletoProfesional(profesional.getNombre() + " " + profesional.getApellidos());
        dto.setEspecialidadProfesional(profesional.getEspecialidad() != null ? profesional.getEspecialidad().getNombre() : null);

        // Información de la clínica
        if (documento.getTenantId() != null) {
            LOGGER.info("Buscando clínica para tenantId: " + documento.getTenantId());
            Optional<clinica> clinicaOpt = clinicaDAO.findByTenantId(documento.getTenantId());
            if (clinicaOpt.isPresent()) {
                String nombreClinica = clinicaOpt.get().getNombre();
                dto.setNombreClinica(nombreClinica);
                LOGGER.info("Clínica encontrada: '" + nombreClinica + "' para tenantId: " + documento.getTenantId());
            } else {
                LOGGER.warning("No se encontró clínica para tenantId: " + documento.getTenantId() + " del documento " + documento.getId());
            }
        } else {
            LOGGER.warning("Documento " + documento.getId() + " no tiene tenantId");
        }

        // Motivo de consulta
        dto.setCodigoMotivoConsulta(documento.getCodigoMotivoConsulta());
        documentoDAO.getNombreMotivoConsulta(documento.getCodigoMotivoConsulta())
                .ifPresent(dto::setNombreMotivoConsulta);

        // Diagnóstico
        dto.setDescripcionDiagnostico(documento.getDescripcionDiagnostico());
        dto.setFechaInicioDiagnostico(documento.getFechaInicioDiagnostico());
        dto.setCodigoEstadoProblema(documento.getCodigoEstadoProblema());
        if (documento.getCodigoEstadoProblema() != null) {
            documentoDAO.getNombreEstadoProblema(documento.getCodigoEstadoProblema())
                    .ifPresent(dto::setNombreEstadoProblema);
        }
        dto.setCodigoGradoCerteza(documento.getCodigoGradoCerteza());
        documentoDAO.getNombreGradoCerteza(documento.getCodigoGradoCerteza())
                .ifPresent(dto::setNombreGradoCerteza);

        // Instrucciones de seguimiento
        dto.setFechaProximaConsulta(documento.getFechaProximaConsulta());
        dto.setDescripcionProximaConsulta(documento.getDescripcionProximaConsulta());
        dto.setReferenciaAlta(documento.getReferenciaAlta());

        return dto;
    }

    /**
     * Convierte una lista de documentos a DTOs con caché (optimizado para batch)
     * Evita N+1 queries haciendo consultas batch para clínicas y codigueras
     */
    private List<documento_clinico_dto> convertirListaADTOConCache(List<documento_clinico> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Extraer todos los tenantIds únicos
        Set<UUID> tenantIds = documentos.stream()
            .map(documento_clinico::getTenantId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 2. Extraer todos los códigos de codigueras únicos (solo los que se usan en estos documentos)
        Set<String> codigosMotivo = documentos.stream()
            .map(documento_clinico::getCodigoMotivoConsulta)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<String> codigosEstado = documentos.stream()
            .map(documento_clinico::getCodigoEstadoProblema)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<String> codigosGrado = documentos.stream()
            .map(documento_clinico::getCodigoGradoCerteza)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 3. Hacer UNA consulta batch por cada tipo (solo trae los nombres necesarios)
        // UNA sola consulta SQL con IN clause para clínicas
        Map<UUID, String> clinicasCache = clinicaDAO.getNombresClinicasBatch(tenantIds);

        // UNA sola consulta SQL con IN clause para cada tipo de codiguera
        Map<String, String> motivosCache = documentoDAO.getNombresMotivosConsultaBatch(codigosMotivo);
        Map<String, String> estadosCache = documentoDAO.getNombresEstadosProblemaBatch(codigosEstado);
        Map<String, String> gradosCache = documentoDAO.getNombresGradosCertezaBatch(codigosGrado);

        LOGGER.info(String.format("Batch conversion: %d docs, %d clinics, %d motivos, %d estados, %d grados",
            documentos.size(), clinicasCache.size(), motivosCache.size(), estadosCache.size(), gradosCache.size()));

        // 4. Convertir cada documento usando los cachés
        return documentos.stream()
            .map(doc -> convertirADTOConCache(doc, clinicasCache, motivosCache, estadosCache, gradosCache))
            .collect(Collectors.toList());
    }

    /**
     * Convierte un documento a DTO usando cachés de clínicas y codigueras
     */
    private documento_clinico_dto convertirADTOConCache(documento_clinico documento,
                                                        Map<UUID, String> clinicasCache,
                                                        Map<String, String> motivosCache,
                                                        Map<String, String> estadosCache,
                                                        Map<String, String> gradosCache) {
        documento_clinico_dto dto = new documento_clinico_dto();

        // IDs y fechas
        dto.setId(documento.getId().toString());
        dto.setTenantId(documento.getTenantId() != null ? documento.getTenantId().toString() : null);
        dto.setFecCreacion(documento.getFecCreacion());

        // Relaciones
        dto.setUsuarioSaludCedula(documento.getUsuarioSaludCedula());
        dto.setProfesionalCi(documento.getProfesionalFirmante().getCi());

        // Información del paciente
        if (documento.getPaciente() != null) {
            dto.setNombreCompletoPaciente(documento.getPaciente().getNombreCompleto());
        }

        // Información del profesional
        profesional_salud profesional = documento.getProfesionalFirmante();
        dto.setNombreCompletoProfesional(profesional.getNombre() + " " + profesional.getApellidos());
        dto.setEspecialidadProfesional(profesional.getEspecialidad() != null ? profesional.getEspecialidad().getNombre() : null);

        // Información de la clínica usando caché (NO hace consulta a BD)
        if (documento.getTenantId() != null) {
            dto.setNombreClinica(clinicasCache.get(documento.getTenantId()));
        }

        // Motivo de consulta usando caché (NO hace consulta a BD)
        dto.setCodigoMotivoConsulta(documento.getCodigoMotivoConsulta());
        if (documento.getCodigoMotivoConsulta() != null) {
            dto.setNombreMotivoConsulta(motivosCache.get(documento.getCodigoMotivoConsulta()));
        }

        // Diagnóstico
        dto.setDescripcionDiagnostico(documento.getDescripcionDiagnostico());
        dto.setFechaInicioDiagnostico(documento.getFechaInicioDiagnostico());

        // Estado de problema usando caché (NO hace consulta a BD)
        dto.setCodigoEstadoProblema(documento.getCodigoEstadoProblema());
        if (documento.getCodigoEstadoProblema() != null) {
            dto.setNombreEstadoProblema(estadosCache.get(documento.getCodigoEstadoProblema()));
        }

        // Grado de certeza usando caché (NO hace consulta a BD)
        dto.setCodigoGradoCerteza(documento.getCodigoGradoCerteza());
        if (documento.getCodigoGradoCerteza() != null) {
            dto.setNombreGradoCerteza(gradosCache.get(documento.getCodigoGradoCerteza()));
        }

        // Instrucciones de seguimiento
        dto.setFechaProximaConsulta(documento.getFechaProximaConsulta());
        dto.setDescripcionProximaConsulta(documento.getDescripcionProximaConsulta());
        dto.setReferenciaAlta(documento.getReferenciaAlta());

        return dto;
    }

    /**
     * Valida campos obligatorios
     */
    private void validarCamposObligatorios(
            String usuarioSaludCedula, Integer profesionalCi, String codigoMotivoConsulta,
            String descripcionDiagnostico, LocalDate fechaInicioDiagnostico,
            String codigoGradoCerteza, UUID tenantId) {

        if (usuarioSaludCedula != null && usuarioSaludCedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula del paciente no puede estar vacía");
        }
        if (profesionalCi != null && profesionalCi <= 0) {
            throw new IllegalArgumentException("La CI del profesional debe ser válida");
        }
        if (codigoMotivoConsulta == null || codigoMotivoConsulta.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de consulta es requerido");
        }
        if (descripcionDiagnostico == null || descripcionDiagnostico.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del diagnóstico es requerida");
        }
        if (fechaInicioDiagnostico == null) {
            throw new IllegalArgumentException("La fecha de inicio del diagnóstico es requerida");
        }
        if (codigoGradoCerteza == null || codigoGradoCerteza.trim().isEmpty()) {
            throw new IllegalArgumentException("El grado de certeza es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
    }

    /**
     * Valida que las codigueras existan en la base de datos
     */
    private void validarCodigueras(String codigoMotivoConsulta, String codigoEstadoProblema, String codigoGradoCerteza) {
        // Validar motivo de consulta
        if (documentoDAO.getNombreMotivoConsulta(codigoMotivoConsulta).isEmpty()) {
            throw new IllegalArgumentException("El código de motivo de consulta '" + codigoMotivoConsulta + "' no es válido");
        }

        // Validar estado de problema (opcional)
        if (codigoEstadoProblema != null && !codigoEstadoProblema.trim().isEmpty()) {
            if (documentoDAO.getNombreEstadoProblema(codigoEstadoProblema).isEmpty()) {
                throw new IllegalArgumentException("El código de estado de problema '" + codigoEstadoProblema + "' no es válido");
            }
        }

        // Validar grado de certeza
        if (documentoDAO.getNombreGradoCerteza(codigoGradoCerteza).isEmpty()) {
            throw new IllegalArgumentException("El código de grado de certeza '" + codigoGradoCerteza + "' no es válido");
        }
    }

    /**
     * Normaliza el tamaño de página
     */
    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Envía un documento recién creado a la cola JMS para sincronización asíncrona con el componente central.
     * Registra el envío en la tabla sincronizacion_pendiente para auditoría y monitoreo.
     *
     * Este método NO intenta sincronizar directamente vía REST. En su lugar:
     * 1. Envía mensaje a cola "DocumentosSincronizacion"
     * 2. Registra en tabla de auditoría (sincronizacion_pendiente)
     * 3. El componente central consumirá el mensaje y procesará el documento
     * 4. El central enviará confirmación a cola "SincronizacionConfirmaciones"
     * 5. El consumidor de confirmaciones actualizará hist_clinica_id y estado de auditoría
     *
     * @param documento Documento recién creado
     * @param tenantId ID de la clínica
     */
    private void enviarDocumentoACola(documento_clinico documento, UUID tenantId) {

        LOGGER.log(Level.INFO, "Enviando documento {0} a cola de sincronización (paciente: {1}, tenant: {2})",
                new Object[]{documento.getId(), documento.getUsuarioSaludCedula(), tenantId});

        String messageId = null;

        try {
            // Enviar mensaje JMS a la cola
            messageId = sincronizacionProducer.enviarDocumento(
                    documento.getId(),
                    documento.getUsuarioSaludCedula(),
                    tenantId
            );

            LOGGER.log(Level.INFO, "Documento {0} enviado a cola exitosamente. MessageID: {1}",
                    new Object[]{documento.getId(), messageId});

        } catch (JMSException e) {
            // Error crítico: no se pudo enviar a la cola
            LOGGER.log(Level.SEVERE, "Error al enviar documento " + documento.getId() +
                    " a cola de sincronización", e);

            // Continuar para registrar en tabla de auditoría con estado ERROR
        }

        // Registrar en tabla de auditoría para monitoreo
        // Se crea SIEMPRE, independientemente si el envío a cola fue exitoso o no
        try {
            SincronizacionPendiente auditoria = new SincronizacionPendiente(
                    documento.getUsuarioSaludCedula(),
                    tenantId,
                    documento.getId() // documento_id
            );

            auditoria.setMessageId(messageId);
            auditoria.setFecEnvioCola(LocalDateTime.now());

            if (messageId != null) {
                // Mensaje enviado exitosamente
                auditoria.setEstado(SincronizacionPendiente.EstadoSincronizacion.PENDIENTE);
            } else {
                // Falló envío a cola
                auditoria.setEstado(SincronizacionPendiente.EstadoSincronizacion.ERROR);
                auditoria.registrarError("Error al enviar mensaje a cola JMS");
            }

            sincronizacionDAO.save(auditoria);

            LOGGER.log(Level.INFO, "Registro de auditoría creado para documento {0}",
                    documento.getId());

        } catch (Exception e) {
            // Error al guardar auditoría - loguear pero no fallar la transacción principal
            LOGGER.log(Level.SEVERE, "Error al crear registro de auditoría para documento " +
                    documento.getId(), e);
        }
    }

    // ========== MÉTODOS PARA VALIDACIÓN DE PERMISOS DE ACCESO ==========

    /**
     * Valida si un profesional tiene permiso para acceder a un documento clínico
     * Llama al componente central para verificar políticas de acceso
     *
     * @param documentoId UUID del documento
     * @param ciProfesional CI del profesional que solicita acceso
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional (opcional, se busca automáticamente si es null)
     * @return true si tiene permiso, false en caso contrario
     */
    public boolean validarAccesoDocumento(UUID documentoId, Integer ciProfesional, UUID tenantId, String especialidad) {
        if (documentoId == null || ciProfesional == null || tenantId == null) {
            LOGGER.warning("Parámetros inválidos para validar acceso");
            return false;
        }

        try {
            // 1. Buscar el profesional por CI y tenantId para obtener su UUID y especialidad
            Optional<profesional_salud> profesionalOpt = profesionalDAO.findByCiAndTenantId(ciProfesional, tenantId);
            if (profesionalOpt.isEmpty()) {
                LOGGER.warning(String.format("Profesional no encontrado: CI=%d, tenant=%s", ciProfesional, tenantId));
                return false;
            }

            profesional_salud profesional = profesionalOpt.get();

            // 2. Si no se proporcionó especialidad, buscarla del profesional
            String especialidadNombre = especialidad;
            if (especialidadNombre == null || especialidadNombre.isBlank()) {
                if (profesional.getEspecialidad() != null) {
                    especialidadNombre = profesional.getEspecialidad().getNombre();
                }
            }

            // 3. Consultar el documento localmente
            Optional<documento_clinico> documentoOpt = documentoDAO.findById(documentoId);
            if (documentoOpt.isPresent()) {
                documento_clinico documento = documentoOpt.get();

                // 4. Verificar si el profesional actual es el creador del documento
                if (documento.getProfesionalId() != null &&
                    documento.getProfesionalId().equals(profesional.getId())) {
                    LOGGER.info("Acceso automático concedido: el profesional CI=" + ciProfesional +
                              " es el creador del documento " + documentoId);
                    return true; // El creador siempre tiene acceso permanente
                }
            }

            // 5. Si no es el creador, validar con el sistema de permisos del central
            return centralAPIClient.validarAccesoDocumento(documentoId, ciProfesional, tenantId, especialidadNombre);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar acceso a documento", e);
            return false; // En caso de error, denegar acceso por seguridad
        }
    }

    /**
     * Valida si un profesional tiene permiso para acceder a múltiples documentos (batch)
     * Optimización para evitar N+1 HTTP calls
     *
     * @param documentoIds Lista de UUIDs de documentos
     * @param ciProfesional CI del profesional
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional (opcional)
     * @return Map con documentoId como key y boolean (tiene permiso) como value
     */
    public Map<UUID, Boolean> validarAccesoDocumentos(List<UUID> documentoIds, Integer ciProfesional,
                                                        UUID tenantId, String especialidad) {
        Map<UUID, Boolean> resultado = new HashMap<>();

        if (documentoIds == null || documentoIds.isEmpty()) {
            return resultado;
        }

        if (ciProfesional == null || tenantId == null) {
            LOGGER.warning("Parámetros inválidos para validar acceso batch");
            // Denegar todos por seguridad
            documentoIds.forEach(id -> resultado.put(id, false));
            return resultado;
        }

        try {
            // 1. Buscar el profesional UNA sola vez
            Optional<profesional_salud> profesionalOpt = profesionalDAO.findByCiAndTenantId(ciProfesional, tenantId);
            if (profesionalOpt.isEmpty()) {
                LOGGER.warning(String.format("Profesional no encontrado: CI=%d, tenant=%s", ciProfesional, tenantId));
                // Denegar todos
                documentoIds.forEach(id -> resultado.put(id, false));
                return resultado;
            }

            profesional_salud profesional = profesionalOpt.get();

            // 2. Obtener especialidad si no se proporcionó
            String especialidadNombre = especialidad;
            if (especialidadNombre == null || especialidadNombre.isBlank()) {
                if (profesional.getEspecialidad() != null) {
                    especialidadNombre = profesional.getEspecialidad().getNombre();
                }
            }

            // 3. Consultar documentos localmente BATCH
            List<documento_clinico> documentosLocal = documentoDAO.findByIdsAndTenantId(documentoIds, tenantId);

            // 4. Separar documentos creados por el profesional (auto-granted) de los demás
            Set<UUID> documentosCreados = new HashSet<>();
            List<UUID> documentosAValidarConCentral = new ArrayList<>();

            for (UUID docId : documentoIds) {
                boolean esCreador = documentosLocal.stream()
                    .filter(doc -> doc.getId().equals(docId))
                    .anyMatch(doc -> doc.getProfesionalId() != null &&
                                     doc.getProfesionalId().equals(profesional.getId()));

                if (esCreador) {
                    documentosCreados.add(docId);
                    resultado.put(docId, true); // Creador siempre tiene acceso
                } else {
                    documentosAValidarConCentral.add(docId);
                }
            }

            LOGGER.info(String.format("Validación batch: %d documentos creados por CI=%d, %d requieren validación central",
                documentosCreados.size(), ciProfesional, documentosAValidarConCentral.size()));

            // 5. Validar con central solo los que no son creados por este profesional (BATCH)
            if (!documentosAValidarConCentral.isEmpty()) {
                Map<UUID, Boolean> permisosCentral = centralAPIClient.validarAccesoDocumentos(
                    documentosAValidarConCentral, ciProfesional, tenantId, especialidadNombre);

                resultado.putAll(permisosCentral);
            }

            return resultado;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar acceso batch a documentos", e);
            // En caso de error, denegar todos por seguridad
            documentoIds.forEach(id -> resultado.put(id, false));
            return resultado;
        }
    }

    /**
     * Solicita acceso a un documento clínico enviando notificación al paciente
     * Solo permite una solicitud por minuto por profesional-documento
     *
     * @param documentoId UUID del documento
     * @param ciProfesional CI del profesional solicitante
     * @param nombreProfesional Nombre completo del profesional
     * @param especialidad Especialidad del profesional
     * @param tenantId UUID de la clínica
     * @return ResultadoSolicitudAcceso indicando si fue exitoso o ya existe una solicitud reciente
     * @throws IllegalArgumentException si el documento no existe
     */
    public ResultadoSolicitudAcceso solicitarAccesoDocumento(
            UUID documentoId,
            Integer ciProfesional,
            String nombreProfesional,
            String especialidad,
            UUID tenantId) {

        // Validar que no existe una solicitud reciente
        if (!solicitudAccesoDAO.puedeVolverASolicitar(documentoId, ciProfesional, tenantId)) {
            LOGGER.info(String.format(
                "Solicitud duplicada rechazada (cooldown 1m): documento=%s, profesional=%d",
                documentoId, ciProfesional));

            return new ResultadoSolicitudAcceso(
                false,
                "Ya existe una solicitud de acceso reciente para este documento. " +
                "Por favor espere 1 minuto antes de volver a solicitar."
            );
        }

        // Obtener información del documento
        Optional<documento_clinico> docOpt = documentoDAO.findById(documentoId);
        if (docOpt.isEmpty()) {
            throw new IllegalArgumentException("No se encontró el documento con ID " + documentoId);
        }

        documento_clinico documento = docOpt.get();
        String cedulaPaciente = documento.getUsuarioSaludCedula();

        // Obtener nombre de la clínica
        Optional<clinica> clinicaOpt = clinicaDAO.findByTenantId(tenantId);
        String nombreClinica = clinicaOpt.map(clinica::getNombre).orElse("Clínica desconocida");

        // Formatear datos del documento para la notificación
        String fechaDocumento = documento.getFecCreacion() != null ?
            documento.getFecCreacion().toLocalDate().toString() : "Fecha desconocida";
        String motivoConsulta = documento.getCodigoMotivoConsulta() != null ?
            documento.getCodigoMotivoConsulta() : "No especificado";
        String diagnostico = documento.getDescripcionDiagnostico();

        // Buscar la especialidad del profesional en la base de datos
        String especialidadNombre = null;
        UUID especialidadId = null;
        Optional<profesional_salud> profesionalOpt = profesionalDAO.findByCiAndTenantId(ciProfesional, tenantId);

        if (profesionalOpt.isPresent()) {
            profesional_salud profesional = profesionalOpt.get();
            if (profesional.getEspecialidad() != null) {
                especialidadNombre = profesional.getEspecialidad().getNombre();
                especialidadId = profesional.getEspecialidad().getId();
            }
        }

        // PRIMERO: Registrar solicitud en base de datos local para capturar su ID
        // Esto genera el UUID que necesitamos enviar al componente central
        com.hcen.periferico.entity.solicitud_acceso_documento solicitud =
            solicitudAccesoDAO.registrarSolicitud(documentoId, ciProfesional, tenantId, cedulaPaciente);

        UUID solicitudId = solicitud.getId(); // Capturar el UUID generado

        // SEGUNDO: Enviar notificación al paciente vía componente central incluyendo el solicitudId
        boolean notificacionEnviada = centralAPIClient.solicitarAccesoDocumento(
            solicitudId,
            cedulaPaciente,
            documentoId,
            ciProfesional,
            nombreProfesional,
            especialidadNombre,
            especialidadId,
            tenantId,
            nombreClinica,
            fechaDocumento,
            motivoConsulta,
            diagnostico
        );

        if (!notificacionEnviada) {
            LOGGER.warning(String.format(
                "No se pudo enviar la notificación de solicitud de acceso (documento=%s, profesional=%d, paciente=%s, solicitudId=%s)",
                documentoId, ciProfesional, cedulaPaciente, solicitudId));

            return new ResultadoSolicitudAcceso(
                false,
                "No se pudo enviar la notificación al paciente. Por favor verifique los datos o intente nuevamente."
            );
        }

        LOGGER.info(String.format(
            "Solicitud de acceso registrada: documento=%s, profesional=%d, paciente=%s",
            documentoId, ciProfesional, cedulaPaciente));

        return new ResultadoSolicitudAcceso(
            true,
            "Solicitud de acceso enviada exitosamente. El paciente recibirá una notificación."
        );
    }

    /**
     * Clase para representar el resultado de una solicitud de acceso
     */
    public static class ResultadoSolicitudAcceso {
        private final boolean exitoso;
        private final String mensaje;

        public ResultadoSolicitudAcceso(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public boolean isExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
