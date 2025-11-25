package hcen.central.inus.service;

import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.PoliticaAccesoDAO;
import hcen.central.inus.dto.PoliticaAccesoDTO;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.politica_acceso;
import hcen.central.inus.enums.EstadoPermiso;
import hcen.central.inus.enums.TipoPermiso;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service para gestión de políticas de acceso a documentos clínicos
 */
@Stateless
public class PoliticaAccesoService {

    private static final Logger LOGGER = Logger.getLogger(PoliticaAccesoService.class.getName());

    private static final int DIAS_EXPIRACION_DEFAULT = 15;

    @EJB
    private PoliticaAccesoDAO politicaDAO;

    @EJB
    private HistoriaClinicaDAO historiaDAO;

    /**
     * Otorga un permiso de acceso a un documento clínico
     *
     * @param dto DTO con los datos del permiso
     * @return DTO del permiso creado
     */
    public PoliticaAccesoDTO otorgarPermiso(PoliticaAccesoDTO dto) {
        validarDatosPermiso(dto);

        // Verificar que existe la historia clínica
        historia_clinica historia = historiaDAO.findById(dto.getHistoriaClinicaId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No existe la historia clínica con ID " + dto.getHistoriaClinicaId()));

        // Crear entidad
        politica_acceso politica = new politica_acceso();
        politica.setHistoriaClinica(historia);
        politica.setDocumentoId(dto.getDocumentoId());
        politica.setTipoPermiso(dto.getTipoPermiso());
        politica.setCiProfesional(dto.getCiProfesional());
        politica.setTenantId(dto.getTenantId());
        politica.setEspecialidad(dto.getEspecialidad());
        politica.setFechaOtorgamiento(LocalDateTime.now());

        // Calcular fecha de expiración (por defecto +15 días)
        LocalDateTime fechaExpiracion = dto.getFechaExpiracion();
        if (fechaExpiracion == null) {
            fechaExpiracion = LocalDateTime.now().plusDays(DIAS_EXPIRACION_DEFAULT);
        }
        politica.setFechaExpiracion(fechaExpiracion);
        politica.setEstado(EstadoPermiso.ACTIVO);

        // Persistir
        politica = politicaDAO.save(politica);

        LOGGER.info(String.format("Permiso otorgado: tipo=%s, documento=%s, tenant=%s",
            dto.getTipoPermiso(), dto.getDocumentoId(), dto.getTenantId()));

        return new PoliticaAccesoDTO(politica);
    }

    /**
     * Valida si un profesional tiene permiso para acceder a un documento
     *
     * @param documentoId UUID del documento
     * @param ciProfesional CI del profesional
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional
     * @return true si tiene permiso, false en caso contrario
     */
    public boolean validarAcceso(UUID documentoId, Integer ciProfesional, UUID tenantId, String especialidad) {
        if (documentoId == null) {
            throw new IllegalArgumentException("El documentoId es requerido");
        }
        if (ciProfesional == null) {
            throw new IllegalArgumentException("El CI del profesional es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenantId es requerido");
        }

        boolean tienePermiso = politicaDAO.tienePermisoAcceso(documentoId, ciProfesional, tenantId, especialidad);

        LOGGER.info(String.format("Validación de acceso: documento=%s, profesional=%s, tenant=%s, resultado=%s",
            documentoId, ciProfesional, tenantId, tienePermiso));

        return tienePermiso;
    }

    /**
     * Valida si un profesional tiene permiso para acceder a múltiples documentos (batch)
     *
     * @param documentoIds Lista de UUIDs de documentos
     * @param ciProfesional CI del profesional
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional
     * @return Map con documentoId como key y boolean (tiene permiso) como value
     */
    public Map<UUID, Boolean> validarAccesoBatch(List<UUID> documentoIds, Integer ciProfesional,
                                                   UUID tenantId, String especialidad) {
        if (documentoIds == null || documentoIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de documentoIds es requerida y no puede estar vacía");
        }
        if (ciProfesional == null) {
            throw new IllegalArgumentException("El CI del profesional es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenantId es requerido");
        }

        Map<UUID, Boolean> permisos = politicaDAO.tienePermisoAccesoBatch(documentoIds, ciProfesional, tenantId, especialidad);

        LOGGER.info(String.format("Validación batch de acceso: %d documentos, profesional=%s, tenant=%s",
            documentoIds.size(), ciProfesional, tenantId));

        return permisos;
    }

    /**
     * Revoca un permiso antes de su fecha de expiración
     *
     * @param permisoId UUID del permiso
     * @param motivo Motivo de la revocación
     */
    public void revocarPermiso(UUID permisoId, String motivo) {
        if (permisoId == null) {
            throw new IllegalArgumentException("El ID del permiso es requerido");
        }

        politica_acceso politica = politicaDAO.findById(permisoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el permiso con ID " + permisoId));

        if (politica.estaRevocado()) {
            throw new IllegalStateException("El permiso ya fue revocado anteriormente");
        }

        if (politica.estaExpirado()) {
            throw new IllegalStateException("El permiso ya expiró, no puede ser revocado");
        }

        politica.revocar(motivo != null ? motivo : "Revocado por el paciente");
        politicaDAO.save(politica);

        LOGGER.info(String.format("Permiso revocado: id=%s, motivo=%s", permisoId, motivo));
    }

    /**
     * Lista todos los permisos de una historia clínica
     *
     * @param historiaClinicaId UUID de la historia clínica
     * @return Lista de DTOs de permisos
     */
    public List<PoliticaAccesoDTO> listarPermisosPaciente(UUID historiaClinicaId) {
        if (historiaClinicaId == null) {
            throw new IllegalArgumentException("El ID de la historia clínica es requerido");
        }

        List<politica_acceso> politicas = politicaDAO.listarPorHistoria(historiaClinicaId);
        return politicas.stream()
            .map(PoliticaAccesoDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Lista solo los permisos activos de una historia clínica
     *
     * @param historiaClinicaId UUID de la historia clínica
     * @return Lista de DTOs de permisos activos
     */
    public List<PoliticaAccesoDTO> listarPermisosActivos(UUID historiaClinicaId) {
        if (historiaClinicaId == null) {
            throw new IllegalArgumentException("El ID de la historia clínica es requerido");
        }

        List<politica_acceso> politicas = politicaDAO.listarActivasPorHistoria(historiaClinicaId);
        return politicas.stream()
            .map(PoliticaAccesoDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Lista los permisos activos de un documento específico
     *
     * @param documentoId UUID del documento
     * @return Lista de DTOs de permisos activos
     */
    public List<PoliticaAccesoDTO> listarPermisosDocumento(UUID documentoId) {
        if (documentoId == null) {
            throw new IllegalArgumentException("El ID del documento es requerido");
        }

        List<politica_acceso> politicas = politicaDAO.listarActivasPorDocumento(documentoId);
        return politicas.stream()
            .map(PoliticaAccesoDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Marca como expirados todos los permisos vencidos
     * Método llamado por un scheduler/job periódicamente
     *
     * @return Cantidad de permisos marcados como expirados
     */
    public int expirarPermisosVencidos() {
        int cantidadExpirados = politicaDAO.expirarPermisosVencidos();
        LOGGER.info(String.format("Permisos expirados: %d", cantidadExpirados));
        return cantidadExpirados;
    }

    /**
     * Cuenta la cantidad de permisos activos para un documento
     */
    public long contarPermisosActivos(UUID documentoId) {
        if (documentoId == null) {
            throw new IllegalArgumentException("El ID del documento es requerido");
        }
        return politicaDAO.contarPermisosActivos(documentoId);
    }

    /**
     * Extiende la fecha de expiración de un permiso activo
     *
     * @param permisoId UUID del permiso
     * @param nuevaFechaExpiracion Nueva fecha de expiración
     * @return DTO del permiso actualizado
     */
    public PoliticaAccesoDTO extenderExpiracion(UUID permisoId, LocalDateTime nuevaFechaExpiracion) {
        if (permisoId == null) {
            throw new IllegalArgumentException("El ID del permiso es requerido");
        }
        if (nuevaFechaExpiracion == null) {
            throw new IllegalArgumentException("La nueva fecha de expiración es requerida");
        }
        if (nuevaFechaExpiracion.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de expiración debe ser futura");
        }

        politica_acceso politica = politicaDAO.findById(permisoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el permiso con ID " + permisoId));

        if (politica.estaRevocado()) {
            throw new IllegalStateException("No se puede extender un permiso revocado");
        }

        if (politica.estaExpirado()) {
            throw new IllegalStateException("No se puede extender un permiso expirado");
        }

        LocalDateTime fechaAnterior = politica.getFechaExpiracion();
        politica.setFechaExpiracion(nuevaFechaExpiracion);
        politica = politicaDAO.save(politica);

        LOGGER.info(String.format("Expiración extendida: permiso=%s, fechaAnterior=%s, fechaNueva=%s",
            permisoId, fechaAnterior, nuevaFechaExpiracion));

        return new PoliticaAccesoDTO(politica);
    }

    /**
     * Modifica el tipo de permiso de una política de acceso existente
     *
     * @param permisoId UUID del permiso
     * @param nuevoTipo Nuevo tipo de permiso
     * @param ciProfesional CI del profesional (requerido si tipo = PROFESIONAL_ESPECIFICO)
     * @param especialidad Especialidad (requerida si tipo = POR_ESPECIALIDAD)
     * @return DTO del permiso actualizado
     */
    public PoliticaAccesoDTO modificarTipoPermiso(UUID permisoId, TipoPermiso nuevoTipo,
                                                   Integer ciProfesional, String especialidad) {
        if (permisoId == null) {
            throw new IllegalArgumentException("El ID del permiso es requerido");
        }
        if (nuevoTipo == null) {
            throw new IllegalArgumentException("El nuevo tipo de permiso es requerido");
        }

        // Validaciones específicas por tipo
        if (nuevoTipo == TipoPermiso.PROFESIONAL_ESPECIFICO) {
            if (ciProfesional == null) {
                throw new IllegalArgumentException("El CI del profesional es requerido para permiso específico");
            }
        }

        if (nuevoTipo == TipoPermiso.POR_ESPECIALIDAD) {
            if (especialidad == null || especialidad.isBlank()) {
                throw new IllegalArgumentException("La especialidad es requerida para permiso por especialidad");
            }
        }

        politica_acceso politica = politicaDAO.findById(permisoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el permiso con ID " + permisoId));

        if (politica.estaRevocado()) {
            throw new IllegalStateException("No se puede modificar un permiso revocado");
        }

        if (politica.estaExpirado()) {
            throw new IllegalStateException("No se puede modificar un permiso expirado");
        }

        TipoPermiso tipoAnterior = politica.getTipoPermiso();
        politica.setTipoPermiso(nuevoTipo);

        // Actualizar campos específicos según el tipo
        switch (nuevoTipo) {
            case PROFESIONAL_ESPECIFICO:
                politica.setCiProfesional(ciProfesional);
                politica.setEspecialidad(null); // Limpiar especialidad
                break;
            case POR_ESPECIALIDAD:
                politica.setEspecialidad(especialidad);
                politica.setCiProfesional(null); // Limpiar CI profesional
                break;
            case POR_CLINICA:
                politica.setCiProfesional(null); // Limpiar ambos
                politica.setEspecialidad(null);
                break;
        }

        politica = politicaDAO.save(politica);

        LOGGER.info(String.format("Tipo de permiso modificado: permiso=%s, tipoAnterior=%s, tipoNuevo=%s",
            permisoId, tipoAnterior, nuevoTipo));

        return new PoliticaAccesoDTO(politica);
    }

    /**
     * Valida los datos del permiso antes de crearlo
     */
    private void validarDatosPermiso(PoliticaAccesoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del permiso son requeridos");
        }
        if (dto.getHistoriaClinicaId() == null) {
            throw new IllegalArgumentException("El ID de la historia clínica es requerido");
        }
        if (dto.getDocumentoId() == null) {
            throw new IllegalArgumentException("El ID del documento es requerido");
        }
        if (dto.getTipoPermiso() == null) {
            throw new IllegalArgumentException("El tipo de permiso es requerido");
        }
        if (dto.getTenantId() == null) {
            throw new IllegalArgumentException("El tenant ID (clínica) es requerido");
        }

        // Validaciones específicas según tipo de permiso
        if (dto.getTipoPermiso() == TipoPermiso.PROFESIONAL_ESPECIFICO) {
            if (dto.getCiProfesional() == null) {
                throw new IllegalArgumentException("El CI del profesional es requerido para permiso específico");
            }
        }

        if (dto.getTipoPermiso() == TipoPermiso.POR_ESPECIALIDAD) {
            if (dto.getEspecialidad() == null || dto.getEspecialidad().isBlank()) {
                throw new IllegalArgumentException("La especialidad es requerida para permiso por especialidad");
            }
        }

        // Validar que la fecha de expiración sea futura
        if (dto.getFechaExpiracion() != null && dto.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de expiración debe ser futura");
        }
    }
}
