package hcen.central.inus.api;

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
