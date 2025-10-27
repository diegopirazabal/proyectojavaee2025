package com.hcen.periferico.rest;

import com.hcen.core.domain.administrador_clinica;
import com.hcen.periferico.dto.administrador_clinica_dto;
import com.hcen.periferico.service.AuthenticationService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    @EJB
    private AuthenticationService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            UUID tenantId = UUID.fromString(request.getTenantId());

            administrador_clinica admin = authService.authenticate(
                request.getUsername(),
                request.getPassword(),
                tenantId
            );

            if (admin != null) {
                administrador_clinica_dto dto = toDTO(admin);
                return Response.ok(dto).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Credenciales inválidas"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Tenant ID inválido"))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error en el servidor: " + e.getMessage()))
                .build();
        }
    }

    private administrador_clinica_dto toDTO(administrador_clinica entity) {
        return new administrador_clinica_dto(
            entity.getId(),
            entity.getUsername(),
            entity.getNombre(),
            entity.getApellidos(),
            entity.getTenantId()
        );
    }

    // Clases auxiliares para request/response
    public static class LoginRequest {
        private String username;
        private String password;
        private String tenantId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) { this.error = error; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}