package hcen.central.inus.api;

import hcen.central.inus.dto.SolicitudAccesoNotificacionDTO;
import hcen.central.inus.service.notification_service;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("/notifications")
public class notification_resource {

    @EJB
    private notification_service notificationService;

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