package com.hcen.periferico.service;

import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.dto.documento_clinico_dto;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.UsuarioSalud;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        Optional<profesional_salud> profesionalOpt = profesionalDAO.findByCi(profesionalCi);
        if (profesionalOpt.isEmpty()) {
            throw new IllegalArgumentException("El profesional con CI " + profesionalCi + " no existe");
        }
        profesional_salud profesional = profesionalOpt.get();
        if (!tenantId.equals(profesional.getTenantId())) {
            throw new IllegalArgumentException("El profesional no pertenece a esta clínica");
        }

        // Validar que las codigueras existan
        validarCodigueras(codigoMotivoConsulta, codigoEstadoProblema, codigoGradoCerteza);

        // PRIMERO: Crear y persistir el documento localmente
        // NO asignamos el ID manualmente - Hibernate lo generará automáticamente con @GeneratedValue
        documento_clinico documento = new documento_clinico();
        documento.setTenantId(tenantId);
        documento.setUsuarioSaludCedula(usuarioSaludCedula);
        documento.setPaciente(paciente);
        documento.setProfesionalFirmante(profesional);
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
     * Lista documentos de un paciente
     */
    public List<documento_clinico_dto> getDocumentosPorPaciente(String cedula, UUID tenantId) {
        List<documento_clinico> documentos = documentoDAO.findByPaciente(cedula, tenantId);
        return documentos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista documentos firmados por un profesional
     */
    public List<documento_clinico_dto> getDocumentosPorProfesional(Integer profesionalCi, UUID tenantId) {
        List<documento_clinico> documentos = documentoDAO.findByProfesional(profesionalCi, tenantId);
        return documentos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista documentos con paginación
     */
    public List<documento_clinico_dto> getDocumentosPaginados(UUID tenantId, int page, Integer size) {
        int resolvedSize = normalizePageSize(size);
        List<documento_clinico> documentos = documentoDAO.findAllByTenantIdPaginated(tenantId, page, resolvedSize);
        return documentos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Cuenta documentos de una clínica
     */
    public long countDocumentos(UUID tenantId) {
        return documentoDAO.countByTenantId(tenantId);
    }

    /**
     * Cuenta documentos de un paciente
     */
    public long countDocumentosPorPaciente(String cedula, UUID tenantId) {
        return documentoDAO.countByPaciente(cedula, tenantId);
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
        dto.setEspecialidadProfesional(profesional.getEspecialidad());

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
}
