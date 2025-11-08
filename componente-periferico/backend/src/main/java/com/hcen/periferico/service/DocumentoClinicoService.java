package com.hcen.periferico.service;

import com.hcen.periferico.api.CentralAPIClient;
import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.dto.documento_clinico_dto;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.entity.documento_clinico;
import com.hcen.periferico.entity.profesional_salud;
import com.hcen.periferico.enums.TipoSincronizacion;
import com.hcen.periferico.sync.ICentralSyncAdapter;
import com.hcen.periferico.sync.SyncResult;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

    @EJB
    private CentralAPIClient centralAPIClient;

    /**
     * Adapter para sincronización de DOCUMENTOS con el componente central.
     * Especificamos beanName porque hay múltiples implementaciones de ICentralSyncAdapter.
     */
    @EJB(beanName = "CentralSyncAdapterDocumentos")
    private ICentralSyncAdapter documentosSyncAdapter;

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

        // Sincronización asíncrona con el central
        sincronizarConCentral(documentoGuardado, tenantId);

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
     * Sincroniza el documento con el componente central.
     * Si la sincronización falla, registra el documento en la cola de reintentos.
     *
     * IMPORTANTE: Se sincroniza directamente el documento en lugar de usar el adapter
     * para evitar problemas de visibilidad transaccional (el adapter con REQUIRES_NEW
     * no vería el documento recién creado en la transacción padre).
     */
    private void sincronizarConCentral(documento_clinico documento, UUID tenantId) {
        try {
            System.out.println("=== Iniciando sincronización de documento " + documento.getId() + " ===");

            // Sincronizar directamente con el central (sin usar adapter para evitar problemas transaccionales)
            UUID historiaId = centralAPIClient.registrarDocumentoHistoriaClinica(
                documento.getUsuarioSaludCedula(),
                tenantId,
                documento.getId()
            );

            // Actualizar el documento con el hist_clinica_id retornado
            documento.setHistClinicaId(historiaId);
            documentoDAO.save(documento);

            System.out.println("✓ Documento " + documento.getId() + " sincronizado exitosamente con historia ID " + historiaId);

            // Si existía un registro pendiente para este usuario, marcarlo como resuelto
            sincronizacionDAO.findByUsuarioCedulaAndTenant(
                documento.getUsuarioSaludCedula(),
                tenantId
            ).ifPresent(sync -> {
                // Solo marcar como resuelto si es de tipo DOCUMENTO
                if (sync.getTipo() == TipoSincronizacion.DOCUMENTO) {
                    sync.marcarComoResuelta();
                    sincronizacionDAO.save(sync);
                    System.out.println("✓ Registro de sincronización pendiente marcado como resuelto");
                }
            });

        } catch (Exception e) {
            // Si falla la sincronización, registrar en cola de reintentos
            System.err.println("✗ Error al sincronizar documento " + documento.getId() +
                             " con el central: " + e.getMessage());
            e.printStackTrace();

            // Verificar si ya existe un registro pendiente para este usuario y tipo DOCUMENTO
            Optional<SincronizacionPendiente> syncExistente =
                sincronizacionDAO.findByUsuarioCedulaAndTenant(
                    documento.getUsuarioSaludCedula(),
                    tenantId
                );

            if (syncExistente.isPresent() && syncExistente.get().getTipo() == TipoSincronizacion.DOCUMENTO) {
                // Actualizar registro existente
                SincronizacionPendiente sync = syncExistente.get();
                sync.registrarError("Error al sincronizar documento: " + e.getMessage());
                sync.setDocumentoId(documento.getId());
                sincronizacionDAO.save(sync);
                System.out.println("Actualizado registro de sincronización pendiente existente");
            } else {
                // Crear nuevo registro en la cola con tipo DOCUMENTO
                SincronizacionPendiente nuevaSync = new SincronizacionPendiente(
                    documento.getUsuarioSaludCedula(),
                    tenantId,
                    documento.getId() // documento_id
                );
                nuevaSync.registrarError("Sincronización inicial falló: " + e.getMessage());
                sincronizacionDAO.save(nuevaSync);
                System.out.println("Creado nuevo registro de sincronización pendiente");
            }
        }
    }
}
