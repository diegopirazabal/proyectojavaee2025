package com.hcen.periferico.rest;

import com.hcen.core.domain.administrador_clinica;
import com.hcen.periferico.dto.administrador_clinica_dto;
import com.hcen.periferico.service.AuthenticationService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
            administrador_clinica admin = authService.authenticate(
                request.getUsername(),
                request.getPassword(),
                request.getClinicaRut()
            );

            if (admin != null) {
                administrador_clinica_dto dto = toDTO(admin);
                return Response.ok(dto).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Credenciales inválidas"))
                    .build();
            }
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
            entity.getClinica()
        );
    }

    // Clases auxiliares para request/response
    public static class LoginRequest {
        private String username;
        private String password;
        private String clinicaRut;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getClinicaRut() { return clinicaRut; }
        public void setClinicaRut(String clinicaRut) { this.clinicaRut = clinicaRut; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) { this.error = error; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}