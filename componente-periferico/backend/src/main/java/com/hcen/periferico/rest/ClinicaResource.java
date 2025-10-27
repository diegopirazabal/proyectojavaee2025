package com.hcen.periferico.rest;

import com.hcen.core.domain.clinica;
import com.hcen.periferico.dao.ClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@Path("/clinicas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClinicaResource {

    @EJB
    private ClinicaDAO clinicaDAO;

    /**
     * Lista todas las clínicas disponibles
     * GET /clinicas
     */
    @GET
    public Response getAllClinicas() {
        try {
            List<clinica> clinicas = clinicaDAO.findAll();

            List<ClinicaDTO> clinicasDTO = clinicas.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return Response.ok(clinicasDTO).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener clínicas: " + e.getMessage()))
                .build();
        }
    }

    private ClinicaDTO toDTO(clinica c) {
        return new ClinicaDTO(
            c.getTenantId().toString(),
            c.getNombre()
        );
    }

    // DTOs
    public static class ClinicaDTO {
        private String tenantId;
        private String nombre;

        public ClinicaDTO() {}

        public ClinicaDTO(String tenantId, String nombre) {
            this.tenantId = tenantId;
            this.nombre = nombre;
        }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) { this.error = error; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
