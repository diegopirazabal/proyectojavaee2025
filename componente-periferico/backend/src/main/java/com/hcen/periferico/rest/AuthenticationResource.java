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
            String clinicaRut = (request.getClinicaRut() != null && !request.getClinicaRut().isBlank())
                ? request.getClinicaRut().trim()
                : resolveDefaultClinicaRut();

            if (isDevLoginEnabled()
                && "usuario".equals(request.getUsername())
                && "usuario".equals(request.getPassword())) {
                administrador_clinica_dto dto = new administrador_clinica_dto(
                    java.util.UUID.randomUUID(),
                    request.getUsername(),
                    "Dev",
                    "Usuario",
                    clinicaRut != null ? clinicaRut : "DEV-CLINICA"
                );
                return Response.ok(dto).build();
            }

            administrador_clinica admin = authService.authenticate(
                request.getUsername(),
                request.getPassword(),
                clinicaRut
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

    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        try {
            String clinicaRut = (request.getClinicaRut() != null && !request.getClinicaRut().isBlank())
                ? request.getClinicaRut().trim()
                : resolveDefaultClinicaRut();

            if (clinicaRut == null || clinicaRut.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("clinicaRut requerido (o configurar HCEN_DEFAULT_CLINICA_RUT)"))
                    .build();
            }

            var created = authService.createAdmin(
                request.getUsername(),
                request.getPassword(),
                request.getNombre(),
                request.getApellidos(),
                clinicaRut
            );
            return Response.status(Response.Status.CREATED).entity(toDTO(created)).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(ex.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error en el servidor: " + e.getMessage()))
                .build();
        }
    }

    private String resolveDefaultClinicaRut() {
        String env = System.getenv("HCEN_DEFAULT_CLINICA_RUT");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("hcen.defaultClinicaRut");
        if (prop != null && !prop.isBlank()) return prop.trim();
        return null;
    }

    private boolean isDevLoginEnabled() {
        String env = System.getenv("HCEN_ENABLE_DEV_LOGIN");
        if (env != null) {
            return Boolean.parseBoolean(env);
        }
        return Boolean.parseBoolean(System.getProperty("hcen.enableDevLogin", "true"));
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

    public static class RegisterRequest {
        private String username;
        private String password;
        private String nombre;
        private String apellidos;
        private String clinicaRut; // opcional si se define por env

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getApellidos() { return apellidos; }
        public void setApellidos(String apellidos) { this.apellidos = apellidos; }
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
