package hcen.central.inus.rest;

import hcen.central.inus.dto.AdminLoginRequest;
import hcen.central.inus.entity.admin_hcen;
import hcen.central.inus.service.authentication_service;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminAuthResource {

    private static final Logger LOGGER = Logger.getLogger(AdminAuthResource.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @EJB
    private authentication_service authenticationService;

    @POST
    @Path("/login")
    public Response login(AdminLoginRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                            .add("error", true)
                            .add("message", "username y password son requeridos")
                            .build())
                    .build();
        }

        try {
            admin_hcen admin = authenticationService.authenticate(
                    request.getUsername().trim(),
                    request.getPassword()
            );

            if (admin == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Json.createObjectBuilder()
                                .add("error", true)
                                .add("message", "Credenciales inválidas")
                                .build())
                        .build();
            }

            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("username", admin.getUsername())
                    .add("firstName", admin.getFirstName())
                    .add("lastName", admin.getLastName())
                    .add("email", admin.getEmail())
                    .add("active", Boolean.TRUE.equals(admin.getActive()));

            if (admin.getId() != null) {
                builder.add("id", admin.getId());
            } else {
                builder.addNull("id");
            }

            if (admin.getCreatedAt() != null) {
                builder.add("createdAt", ISO_FORMATTER.format(admin.getCreatedAt()));
            } else {
                builder.addNull("createdAt");
            }

            if (admin.getLastLogin() != null) {
                builder.add("lastLogin", ISO_FORMATTER.format(admin.getLastLogin()));
            } else {
                builder.addNull("lastLogin");
            }

            return Response.ok(builder.build()).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication failed due to server error.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Json.createObjectBuilder()
                            .add("error", true)
                            .add("message", "Error interno al autenticar")
                            .build())
                    .build();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
