package hcen.central.inus.api;

import com.google.gson.Gson;
import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.NotificacionDAO;
import hcen.central.inus.dto.PoliticaAccesoDTO;
import hcen.central.inus.dto.SolicitudAccesoNotificacionDTO;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.notificacion;
import hcen.central.inus.enums.TipoPermiso;
import hcen.central.inus.service.PoliticaAccesoService;
import hcen.central.inus.service.notification_service;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@Path("/notifications")
public class notification_resource {

    private static final Logger LOGGER = Logger.getLogger(notification_resource.class.getName());

    @EJB
    private notification_service notificationService;

    @EJB
    private NotificacionDAO notificacionDAO;

    @EJB
    private PoliticaAccesoService politicaAccesoService;

    @EJB
    private HistoriaClinicaDAO historiaClinicaDAO;

    private final Gson gson = new Gson();

    @POST
    @Path("/broadcast-test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerBroadcastNotification() {
        notificationService.sendBroadcastTestNotification();

        return Response.ok(
                Json.createObjectBuilder()
                        .add("status", "OK")
                        .add("message", "Notificación de prueba enviada a todos los usuarios.")
                        .build()
        ).build();
    }

    @POST
    @Path("/usuarios/{cedula}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendDirectNotification(@PathParam("cedula") String cedula, NotificationRequest request) {
        String message = request != null ? request.getMessage() : null;
        boolean sent = notificationService.sendDirectNotificationToUser(cedula, message);

        if (sent) {
            return Response.ok(
                    Json.createObjectBuilder()
                            .add("status", "OK")
                            .add("message", "Notificación enviada al usuario " + cedula)
                            .build()
            ).build();
        }

        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "No se pudo enviar la notificación al usuario " + cedula)
                        .build())
                .build();
    }

    /**
     * Endpoint para enviar notificación de solicitud de acceso a documento clínico
     * Llamado desde el componente periférico cuando un profesional solicita acceso
     */
    @POST
    @Path("/solicitudes-acceso")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enviarSolicitudAcceso(SolicitudAccesoNotificacionDTO solicitud) {
        if (solicitud == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "Los datos de la solicitud son requeridos")
                            .build())
                    .build();
        }

        if (solicitud.getCedulaPaciente() == null || solicitud.getCedulaPaciente().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "La cédula del paciente es requerida")
                            .build())
                    .build();
        }

        boolean sent = notificationService.enviarNotificacionSolicitudAcceso(solicitud);

        if (sent) {
            return Response.status(Response.Status.CREATED)
                    .entity(Json.createObjectBuilder()
                            .add("status", "OK")
                            .add("message", "Notificación de solicitud de acceso enviada correctamente")
                            .build())
                    .build();
        }

        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "No se pudo enviar la notificación")
                        .build())
                .build();
    }

    /**
     * Endpoint para obtener solicitudes de acceso pendientes de un paciente
     * Llamado desde la app móvil para listar las solicitudes que requieren aprobación/rechazo
     */
    @GET
    @Path("/solicitudes-pendientes/{cedula}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSolicitudesPendientes(@PathParam("cedula") String cedula) {
        try {
            List<notificacion> solicitudes = notificacionDAO.findSolicitudesPendientesByCedula(cedula);

            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (notificacion notif : solicitudes) {
                // Parsear datos adicionales desde JSON
                SolicitudAccesoNotificacionDTO datos = null;
                if (notif.getDatosAdicionales() != null && !notif.getDatosAdicionales().isBlank()) {
                    try {
                        datos = gson.fromJson(notif.getDatosAdicionales(), SolicitudAccesoNotificacionDTO.class);
                    } catch (Exception e) {
                        LOGGER.warning("Error al parsear datos adicionales de notificación " + notif.getId());
                    }
                }

                JsonObjectBuilder objBuilder = Json.createObjectBuilder()
                    .add("id", notif.getId().toString())
                    .add("tipo", notif.getTipo())
                    .add("mensaje", notif.getMensaje())
                    .add("estado", notif.getEstado())
                    .add("fechaCreacion", notif.getFecCreacion().toString());

                // Agregar datos de la solicitud si existen
                if (datos != null) {
                    objBuilder.add("documentoId", datos.getDocumentoId())
                             .add("profesionalCi", datos.getProfesionalCi())
                             .add("profesionalNombre", datos.getProfesionalNombre())
                             .add("especialidad", datos.getEspecialidad() != null ? datos.getEspecialidad() : "")
                             .add("tenantId", datos.getTenantId())
                             .add("nombreClinica", datos.getNombreClinica())
                             .add("fechaDocumento", datos.getFechaDocumento())
                             .add("motivoConsulta", datos.getMotivoConsulta())
                             .add("diagnostico", datos.getDiagnostico() != null ? datos.getDiagnostico() : "");
                }

                arrayBuilder.add(objBuilder);
            }

            return Response.ok(Json.createObjectBuilder()
                .add("status", "OK")
                .add("solicitudes", arrayBuilder)
                .build()).build();

        } catch (Exception e) {
            LOGGER.severe("Error al obtener solicitudes pendientes: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "Error al obtener solicitudes: " + e.getMessage())
                    .build())
                .build();
        }
    }

    /**
     * Endpoint para aprobar una solicitud de acceso a documento clínico
     * Crea el permiso correspondiente y actualiza el estado de la notificación
     */
    @POST
    @Path("/aprobar-solicitud")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobarSolicitud(AprobarSolicitudRequest request) {
        try {
            // Validar request
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "Los datos de la solicitud son requeridos")
                        .build())
                    .build();
            }

            if (request.getNotificacionId() == null || request.getCedulaPaciente() == null ||
                request.getDocumentoId() == null || request.getTipoPermiso() == null ||
                request.getTenantId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "Faltan datos requeridos: notificacionId, cedulaPaciente, documentoId, tipoPermiso, tenantId")
                        .build())
                    .build();
            }

            // Buscar la notificación
            Optional<notificacion> notifOpt = notificacionDAO.findById(UUID.fromString(request.getNotificacionId()));
            if (notifOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "No se encontró la notificación con ID " + request.getNotificacionId())
                        .build())
                    .build();
            }

            notificacion notif = notifOpt.get();

            // Verificar que esté pendiente
            if (!"PENDIENTE".equals(notif.getEstado())) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "La solicitud ya fue procesada anteriormente (estado: " + notif.getEstado() + ")")
                        .build())
                    .build();
            }

            // Buscar o crear historia clínica del paciente
            Optional<historia_clinica> historiaOpt = historiaClinicaDAO.findByCedula(request.getCedulaPaciente());
            UUID historiaClinicaId;
            if (historiaOpt.isPresent()) {
                historiaClinicaId = historiaOpt.get().getId();
            } else {
                // Crear historia clínica si no existe
                historia_clinica nuevaHistoria = new historia_clinica();
                nuevaHistoria.setUsuarioCedula(request.getCedulaPaciente());
                historia_clinica historiaCreada = historiaClinicaDAO.save(nuevaHistoria);
                historiaClinicaId = historiaCreada.getId();
                LOGGER.info("Historia clínica creada para paciente " + request.getCedulaPaciente());
            }

            // Crear el permiso
            PoliticaAccesoDTO dto = new PoliticaAccesoDTO();
            dto.setHistoriaClinicaId(historiaClinicaId);
            dto.setDocumentoId(UUID.fromString(request.getDocumentoId()));
            dto.setTenantId(UUID.fromString(request.getTenantId()));
            dto.setTipoPermiso(TipoPermiso.valueOf(request.getTipoPermiso()));

            // Configurar según tipo de permiso
            if ("PROFESIONAL_ESPECIFICO".equals(request.getTipoPermiso())) {
                if (request.getProfesionalCi() == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "profesionalCi es requerido para permiso específico")
                            .build())
                        .build();
                }
                dto.setCiProfesional(request.getProfesionalCi());
            } else if ("POR_ESPECIALIDAD".equals(request.getTipoPermiso())) {
                if (request.getEspecialidad() == null || request.getEspecialidad().isBlank()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "especialidad es requerida para permiso por especialidad")
                            .build())
                        .build();
                }
                dto.setEspecialidad(request.getEspecialidad());
            }

            String fechaExpiracionRaw = request.getFechaExpiracion();
            if (fechaExpiracionRaw != null && !fechaExpiracionRaw.isBlank()) {
                try {
                    LocalDateTime fechaExpiracion = parseFechaExpiracion(fechaExpiracionRaw.trim());
                    dto.setFechaExpiracion(fechaExpiracion);
                } catch (DateTimeParseException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Json.createObjectBuilder()
                            .add("status", "ERROR")
                            .add("message", "fechaExpiracion tiene un formato inválido. Usar ISO 8601, por ejemplo 2025-01-30T14:30:00Z")
                            .build())
                        .build();
                }
            }

            // Otorgar el permiso
            PoliticaAccesoDTO permisoCreado = politicaAccesoService.otorgarPermiso(dto);

            // Actualizar estado de la notificación
            notif.setEstado("APROBADA");
            notificacionDAO.save(notif);

            LOGGER.info("Solicitud aprobada - Permiso creado con ID: " + permisoCreado.getId());

            return Response.ok(Json.createObjectBuilder()
                .add("status", "OK")
                .add("message", "Solicitud aprobada exitosamente")
                .add("permisoId", permisoCreado.getId().toString())
                .build()).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.severe("Error al aprobar solicitud: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "Error interno: " + e.getMessage())
                    .build())
                .build();
        }
    }

    /**
     * Endpoint para rechazar una solicitud de acceso a documento clínico
     * Actualiza el estado de la notificación a RECHAZADA
     */
    @POST
    @Path("/rechazar-solicitud")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rechazarSolicitud(RechazarSolicitudRequest request) {
        try {
            // Validar request
            if (request == null || request.getNotificacionId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "notificacionId es requerido")
                        .build())
                    .build();
            }

            // Buscar la notificación
            Optional<notificacion> notifOpt = notificacionDAO.findById(UUID.fromString(request.getNotificacionId()));
            if (notifOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "No se encontró la notificación")
                        .build())
                    .build();
            }

            notificacion notif = notifOpt.get();

            // Verificar que esté pendiente
            if (!"PENDIENTE".equals(notif.getEstado())) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("message", "La solicitud ya fue procesada anteriormente")
                        .build())
                    .build();
            }

            // Actualizar estado
            notif.setEstado("RECHAZADA");
            notificacionDAO.save(notif);

            LOGGER.info("Solicitud rechazada - Notificación ID: " + request.getNotificacionId());

            return Response.ok(Json.createObjectBuilder()
                .add("status", "OK")
                .add("message", "Solicitud rechazada exitosamente")
                .build()).build();

        } catch (Exception e) {
            LOGGER.severe("Error al rechazar solicitud: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "Error interno: " + e.getMessage())
                    .build())
                .build();
        }
    }

    private LocalDateTime parseFechaExpiracion(String fechaExpiracion) {
        try {
            return OffsetDateTime.parse(fechaExpiracion).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(fechaExpiracion);
        }
    }

    // ============ CLASES DE REQUEST ============

    public static class AprobarSolicitudRequest {
        private String notificacionId;
        private String cedulaPaciente;
        private String documentoId;
        private String tipoPermiso; // PROFESIONAL_ESPECIFICO | POR_ESPECIALIDAD | POR_CLINICA
        private Integer profesionalCi; // Solo para PROFESIONAL_ESPECIFICO
        private String especialidad; // Solo para POR_ESPECIALIDAD
        private String tenantId;
        private String fechaExpiracion;

        // Getters y Setters
        public String getNotificacionId() { return notificacionId; }
        public void setNotificacionId(String notificacionId) { this.notificacionId = notificacionId; }
        public String getCedulaPaciente() { return cedulaPaciente; }
        public void setCedulaPaciente(String cedulaPaciente) { this.cedulaPaciente = cedulaPaciente; }
        public String getDocumentoId() { return documentoId; }
        public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }
        public String getTipoPermiso() { return tipoPermiso; }
        public void setTipoPermiso(String tipoPermiso) { this.tipoPermiso = tipoPermiso; }
        public Integer getProfesionalCi() { return profesionalCi; }
        public void setProfesionalCi(Integer profesionalCi) { this.profesionalCi = profesionalCi; }
        public String getEspecialidad() { return especialidad; }
        public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getFechaExpiracion() { return fechaExpiracion; }
        public void setFechaExpiracion(String fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    }

    public static class RechazarSolicitudRequest {
        private String notificacionId;
        private String motivo; // Opcional

        public String getNotificacionId() { return notificacionId; }
        public void setNotificacionId(String notificacionId) { this.notificacionId = notificacionId; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }

    public static class NotificationRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
