package hcen.central.inus.api;

import hcen.central.inus.service.notification_service;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
}
