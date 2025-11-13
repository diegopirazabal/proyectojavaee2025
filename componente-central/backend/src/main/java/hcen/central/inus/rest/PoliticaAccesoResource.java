package hcen.central.inus.rest;

import hcen.central.inus.dto.PoliticaAccesoDTO;
import hcen.central.inus.enums.TipoPermiso;
import hcen.central.inus.service.PoliticaAccesoService;
import hcen.central.notifications.dto.ApiResponse;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Resource para gestión de políticas de acceso a documentos clínicos
 */
@Path("/politicas-acceso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PoliticaAccesoResource {

    private static final Logger LOGGER = Logger.getLogger(PoliticaAccesoResource.class.getName());

    @EJB
    private PoliticaAccesoService politicaService;

    /**
     * Otorga un nuevo permiso de acceso a un documento clínico
     * Llamado desde la aplicación móvil cuando el usuario otorga acceso
     */
    @POST
    public Response otorgarPermiso(OtorgarPermisoRequest request) {
        try {
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("El cuerpo de la petición es requerido"))
                    .build();
            }

            // Validar campos requeridos
            validarRequest(request);

            // Crear DTO
            PoliticaAccesoDTO dto = new PoliticaAccesoDTO();
            dto.setHistoriaClinicaId(UUID.fromString(request.getHistoriaClinicaId()));
            dto.setDocumentoId(UUID.fromString(request.getDocumentoId()));
            dto.setTipoPermiso(TipoPermiso.valueOf(request.getTipoPermiso()));
            dto.setTenantId(UUID.fromString(request.getTenantId()));

            // Campos opcionales según tipo de permiso
            if (request.getCiProfesional() != null) {
                dto.setCiProfesional(request.getCiProfesional());
            }
            if (request.getEspecialidad() != null && !request.getEspecialidad().isBlank()) {
                dto.setEspecialidad(request.getEspecialidad());
            }

            // Fecha de expiración (opcional, si no se envía se usa default +15 días)
            if (request.getFechaExpiracion() != null) {
                dto.setFechaExpiracion(LocalDateTime.parse(request.getFechaExpiracion()));
            }

            // Otorgar permiso
            PoliticaAccesoDTO resultado = politicaService.otorgarPermiso(dto);

            return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(resultado))
                .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al otorgar permiso de acceso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al otorgar el permiso"))
                .build();
        }
    }

    /**
     * Valida si un profesional tiene permiso para acceder a un documento
     * Llamado desde el componente periférico antes de mostrar un documento
     */
    @POST
    @Path("/validar")
    public Response validarAcceso(ValidarAccesoRequest request) {
        try {
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("El cuerpo de la petición es requerido"))
                    .build();
            }

            if (request.getDocumentoId() == null || request.getDocumentoId().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("documentoId es requerido"))
                    .build();
            }
            if (request.getCiProfesional() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("ciProfesional es requerido"))
                    .build();
            }
            if (request.getTenantId() == null || request.getTenantId().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("tenantId es requerido"))
                    .build();
            }

            UUID documentoId = UUID.fromString(request.getDocumentoId());
            UUID tenantId = UUID.fromString(request.getTenantId());

            boolean tienePermiso = politicaService.validarAcceso(
                documentoId,
                request.getCiProfesional(),
                tenantId,
                request.getEspecialidad()
            );

            ValidarAccesoResponse response = new ValidarAccesoResponse(tienePermiso);
            return Response.ok(ApiResponse.success(response)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar acceso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al validar el acceso"))
                .build();
        }
    }

    /**
     * Lista todos los permisos de una historia clínica
     */
    @GET
    @Path("/historia/{historiaId}")
    public Response listarPermisosPaciente(@PathParam("historiaId") String historiaId) {
        try {
            UUID historiaUuid = UUID.fromString(historiaId);
            List<PoliticaAccesoDTO> permisos = politicaService.listarPermisosPaciente(historiaUuid);
            return Response.ok(ApiResponse.success(permisos)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("ID de historia clínica inválido"))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al listar permisos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al listar los permisos"))
                .build();
        }
    }

    /**
     * Lista solo los permisos activos de una historia clínica
     */
    @GET
    @Path("/historia/{historiaId}/activos")
    public Response listarPermisosActivos(@PathParam("historiaId") String historiaId) {
        try {
            UUID historiaUuid = UUID.fromString(historiaId);
            List<PoliticaAccesoDTO> permisos = politicaService.listarPermisosActivos(historiaUuid);
            return Response.ok(ApiResponse.success(permisos)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("ID de historia clínica inválido"))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al listar permisos activos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al listar los permisos activos"))
                .build();
        }
    }

    /**
     * Lista los permisos activos de un documento específico
     */
    @GET
    @Path("/documento/{documentoId}")
    public Response listarPermisosDocumento(@PathParam("documentoId") String documentoId) {
        try {
            UUID documentoUuid = UUID.fromString(documentoId);
            List<PoliticaAccesoDTO> permisos = politicaService.listarPermisosDocumento(documentoUuid);
            return Response.ok(ApiResponse.success(permisos)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("ID de documento inválido"))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al listar permisos de documento", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al listar los permisos del documento"))
                .build();
        }
    }

    /**
     * Revoca un permiso antes de su fecha de expiración
     */
    @PUT
    @Path("/{permisoId}/revocar")
    public Response revocarPermiso(@PathParam("permisoId") String permisoId, RevocarPermisoRequest request) {
        try {
            UUID permisoUuid = UUID.fromString(permisoId);
            String motivo = request != null ? request.getMotivo() : null;

            politicaService.revocarPermiso(permisoUuid, motivo);

            return Response.ok(ApiResponse.success("Permiso revocado exitosamente")).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al revocar permiso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al revocar el permiso"))
                .build();
        }
    }

    /**
     * Cuenta la cantidad de permisos activos para un documento
     */
    @GET
    @Path("/documento/{documentoId}/count")
    public Response contarPermisosActivos(@PathParam("documentoId") String documentoId) {
        try {
            UUID documentoUuid = UUID.fromString(documentoId);
            long cantidad = politicaService.contarPermisosActivos(documentoUuid);

            ContarPermisosResponse response = new ContarPermisosResponse(cantidad);
            return Response.ok(ApiResponse.success(response)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("ID de documento inválido"))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al contar permisos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al contar los permisos"))
                .build();
        }
    }

    // Clases de Request/Response

    private void validarRequest(OtorgarPermisoRequest request) {
        if (request.getHistoriaClinicaId() == null || request.getHistoriaClinicaId().isBlank()) {
            throw new IllegalArgumentException("historiaClinicaId es requerido");
        }
        if (request.getDocumentoId() == null || request.getDocumentoId().isBlank()) {
            throw new IllegalArgumentException("documentoId es requerido");
        }
        if (request.getTipoPermiso() == null || request.getTipoPermiso().isBlank()) {
            throw new IllegalArgumentException("tipoPermiso es requerido");
        }
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId es requerido");
        }

        // Validar tipo de permiso
        try {
            TipoPermiso tipo = TipoPermiso.valueOf(request.getTipoPermiso());

            if (tipo == TipoPermiso.PROFESIONAL_ESPECIFICO && request.getCiProfesional() == null) {
                throw new IllegalArgumentException("ciProfesional es requerido para permiso específico");
            }
            if (tipo == TipoPermiso.POR_ESPECIALIDAD &&
                (request.getEspecialidad() == null || request.getEspecialidad().isBlank())) {
                throw new IllegalArgumentException("especialidad es requerida para permiso por especialidad");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No enum constant")) {
                throw new IllegalArgumentException("tipoPermiso inválido. Valores permitidos: PROFESIONAL_ESPECIFICO, POR_ESPECIALIDAD, POR_CLINICA");
            }
            throw e;
        }
    }

    public static class OtorgarPermisoRequest {
        private String historiaClinicaId;
        private String documentoId;
        private String tipoPermiso;
        private Integer ciProfesional;
        private String tenantId;
        private String especialidad;
        private String fechaExpiracion;

        // Getters y Setters
        public String getHistoriaClinicaId() {
            return historiaClinicaId;
        }

        public void setHistoriaClinicaId(String historiaClinicaId) {
            this.historiaClinicaId = historiaClinicaId;
        }

        public String getDocumentoId() {
            return documentoId;
        }

        public void setDocumentoId(String documentoId) {
            this.documentoId = documentoId;
        }

        public String getTipoPermiso() {
            return tipoPermiso;
        }

        public void setTipoPermiso(String tipoPermiso) {
            this.tipoPermiso = tipoPermiso;
        }

        public Integer getCiProfesional() {
            return ciProfesional;
        }

        public void setCiProfesional(Integer ciProfesional) {
            this.ciProfesional = ciProfesional;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getEspecialidad() {
            return especialidad;
        }

        public void setEspecialidad(String especialidad) {
            this.especialidad = especialidad;
        }

        public String getFechaExpiracion() {
            return fechaExpiracion;
        }

        public void setFechaExpiracion(String fechaExpiracion) {
            this.fechaExpiracion = fechaExpiracion;
        }
    }

    public static class ValidarAccesoRequest {
        private String documentoId;
        private Integer ciProfesional;
        private String tenantId;
        private String especialidad;

        // Getters y Setters
        public String getDocumentoId() {
            return documentoId;
        }

        public void setDocumentoId(String documentoId) {
            this.documentoId = documentoId;
        }

        public Integer getCiProfesional() {
            return ciProfesional;
        }

        public void setCiProfesional(Integer ciProfesional) {
            this.ciProfesional = ciProfesional;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getEspecialidad() {
            return especialidad;
        }

        public void setEspecialidad(String especialidad) {
            this.especialidad = especialidad;
        }
    }

    public static class ValidarAccesoResponse {
        private boolean tienePermiso;

        public ValidarAccesoResponse() {
        }

        public ValidarAccesoResponse(boolean tienePermiso) {
            this.tienePermiso = tienePermiso;
        }

        public boolean isTienePermiso() {
            return tienePermiso;
        }

        public void setTienePermiso(boolean tienePermiso) {
            this.tienePermiso = tienePermiso;
        }
    }

    public static class RevocarPermisoRequest {
        private String motivo;

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
    }

    public static class ContarPermisosResponse {
        private long cantidad;

        public ContarPermisosResponse() {
        }

        public ContarPermisosResponse(long cantidad) {
            this.cantidad = cantidad;
        }

        public long getCantidad() {
            return cantidad;
        }

        public void setCantidad(long cantidad) {
            this.cantidad = cantidad;
        }
    }
}
