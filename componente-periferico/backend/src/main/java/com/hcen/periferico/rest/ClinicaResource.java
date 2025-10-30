package com.hcen.periferico.rest;

import com.hcen.core.domain.clinica;
import com.hcen.periferico.dao.ClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    /**
     * Crea una nueva clínica
     * POST /clinicas
     */
    @POST
    public Response createClinica(ClinicaCreateRequest request) {
        try {
            String validationError = validateRequest(request);
            if (validationError != null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(validationError))
                        .build();
            }

            String nombreNormalizado = request.getNombre().trim();
            if (clinicaDAO.findByNombreIgnoreCase(nombreNormalizado).isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("Ya existe una clínica registrada con ese nombre"))
                        .build();
            }

            clinica nuevaClinica = new clinica();
            nuevaClinica.setNombre(nombreNormalizado);
            nuevaClinica.setDireccion(normalize(request.getDireccion()));
            nuevaClinica.setEmail(normalize(request.getEmail()));
            nuevaClinica.setEstado("ACTIVA");
            nuevaClinica.setFecRegistro(LocalDateTime.now(ZoneId.of("America/Montevideo")));

            clinica almacenada = clinicaDAO.save(nuevaClinica);
            return Response.status(Response.Status.CREATED).entity(toDTO(almacenada)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al crear la clínica: " + e.getMessage()))
                    .build();
        }
    }

    private ClinicaDTO toDTO(clinica c) {
        ClinicaDTO dto = new ClinicaDTO();
        dto.setTenantId(c.getTenantId() != null ? c.getTenantId().toString() : null);
        dto.setNombre(c.getNombre());
        dto.setDireccion(c.getDireccion());
        dto.setEmail(c.getEmail());
        dto.setEstado(c.getEstado());
        dto.setFecRegistro(c.getFecRegistro() != null ? c.getFecRegistro().toString() : null);
        return dto;
    }

    private String validateRequest(ClinicaCreateRequest request) {
        if (request == null) {
            return "El cuerpo de la solicitud es obligatorio";
        }

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            return "El nombre de la clínica es obligatorio";
        }
        if (request.getNombre().trim().length() > 150) {
            return "El nombre de la clínica no puede superar los 150 caracteres";
        }

        String email = normalize(request.getEmail());
        if (email != null && !email.isEmpty()) {
            if (email.length() > 150) {
                return "El email no puede superar los 150 caracteres";
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                return "El email de la clínica no tiene un formato válido";
            }
        }

        String direccion = normalize(request.getDireccion());
        if (direccion != null && direccion.length() > 200) {
            return "La dirección no puede superar los 200 caracteres";
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // DTOs
    public static class ClinicaDTO {
        private String tenantId;
        private String nombre;
        private String direccion;
        private String email;
        private String estado;
        private String fecRegistro;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getFecRegistro() { return fecRegistro; }
        public void setFecRegistro(String fecRegistro) { this.fecRegistro = fecRegistro; }
    }

    public static class ClinicaCreateRequest {
        private String nombre;
        private String direccion;
        private String email;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) { this.error = error; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
