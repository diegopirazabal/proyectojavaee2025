package com.hcen.periferico.rest;

import com.hcen.core.domain.profesional_salud;
import com.hcen.periferico.service.ProfesionalAuthenticationService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/auth/profesional")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfesionalAuthenticationResource {

    @EJB
    private ProfesionalAuthenticationService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            UUID tenantId = UUID.fromString(request.getTenantId());
            profesional_salud prof = authService.authenticate(request.getEmail(), request.getPassword(), tenantId);
            if (prof != null) {
                ProfesionalResponse dto = new ProfesionalResponse(prof.getCi(), prof.getNombre(), prof.getApellidos(), tenantId.toString());
                return Response.ok(dto).build();
            }
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("Credenciales inválidas")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("Tenant ID inválido")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse("Error en el servidor: "+e.getMessage())).build();
        }
    }

    public static class LoginRequest {
        private String email; private String password; private String tenantId;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
    public static class ProfesionalResponse {
        private Integer ci; private String nombre; private String apellidos; private String tenantId;
        public ProfesionalResponse() {}
        public ProfesionalResponse(Integer ci, String nombre, String apellidos, String tenantId) { this.ci=ci; this.nombre=nombre; this.apellidos=apellidos; this.tenantId=tenantId; }
        public Integer getCi(){return ci;} public void setCi(Integer v){ci=v;}
        public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
        public String getApellidos(){return apellidos;} public void setApellidos(String v){apellidos=v;}
        public String getTenantId(){return tenantId;} public void setTenantId(String v){tenantId=v;}
    }
    public static class ErrorResponse { private String error; public ErrorResponse(){} public ErrorResponse(String e){error=e;} public String getError(){return error;} public void setError(String e){error=e;} }
}

