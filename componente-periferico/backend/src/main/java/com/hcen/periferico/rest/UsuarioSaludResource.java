package com.hcen.periferico.rest;

import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.service.UsuarioSaludService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Resource simplificado para gestión de usuarios de salud
 * Delega todas las operaciones al componente central (INUS)
 * Base path: /usuarios
 */
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioSaludResource {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludResource.class.getName());

    @EJB
    private UsuarioSaludService usuarioService;

    /**
     * Lista todos los usuarios de una clínica o busca por término
     * GET /usuarios?tenantId={uuid}&search={term}
     */
    @GET
    public Response getUsuarios(@QueryParam("tenantId") String tenantId,
                                @QueryParam("search") String searchTerm) {
        try {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                    .build();
            }

            java.util.List<usuario_salud_dto> usuarios;

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                // Búsqueda con filtro
                usuarios = usuarioService.searchUsuariosByTenantId(searchTerm, tenantId);
            } else {
                // Listar todos
                usuarios = usuarioService.getAllUsuariosByTenantId(tenantId);
            }

            return Response.ok(usuarios).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener usuarios: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Obtiene un usuario por cédula
     * GET /usuarios/{cedula}
     */
    @GET
    @Path("/{cedula}")
    public Response getUsuarioByCedula(@PathParam("cedula") String cedula) {
        try {
            usuario_salud_dto usuario = usuarioService.getUsuarioByCedula(cedula);
            if (usuario != null) {
                return Response.ok(usuario).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Usuario no encontrado"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener usuario: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Registra un usuario en una clínica
     * POST /usuarios/registrar
     */
    @POST
    @Path("/registrar")
    public Response registrarUsuario(RegistrarUsuarioRequest request) {
        try {
            // Validar que se proporcione el tenantId de la clínica
            if (request.getTenantId() == null || request.getTenantId().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El tenant_id de la clínica es requerido"))
                    .build();
            }

            // Convertir tipoDocumento String a enum
            TipoDocumento tipoDoc = request.getTipoDocumento() != null && !request.getTipoDocumento().isEmpty()
                ? TipoDocumento.valueOf(request.getTipoDocumento())
                : TipoDocumento.DO;

            usuario_salud_dto usuario = usuarioService.registrarUsuarioEnClinica(
                request.getCedula(),
                tipoDoc,
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getEmail(),
                request.getFechaNacimiento(),
                request.getTenantId()
            );

            return Response.ok(usuario).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validación fallida: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al registrar usuario: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Desasocia un usuario de una clínica
     * DELETE /usuarios/{cedula}/clinica/{tenantId}
     */
    @DELETE
    @Path("/{cedula}/clinica/{tenantId}")
    public Response deleteUsuario(@PathParam("cedula") String cedula,
                                  @PathParam("tenantId") String tenantId) {
        try {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El tenant_id de la clínica es requerido"))
                    .build();
            }

            boolean deleted = usuarioService.deleteUsuarioDeClinica(cedula, tenantId);

            if (deleted) {
                return Response.ok(new SuccessResponse("Usuario desasociado de la clínica exitosamente")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("No se encontró el usuario en esa clínica"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al eliminar usuario: " + e.getMessage()))
                .build();
        }
    }

    // ==========================
    // DTOs internos
    // ==========================

    public static class RegistrarUsuarioRequest {
        private String cedula;
        private String tipoDocumento;  // DO, PA, OTRO
        private String primerNombre;
        private String segundoNombre;
        private String primerApellido;
        private String segundoApellido;
        private String email;
        private LocalDate fechaNacimiento;
        private String tenantId;

        // Getters y Setters
        public String getCedula() { return cedula; }
        public void setCedula(String cedula) { this.cedula = cedula; }

        public String getTipoDocumento() { return tipoDocumento; }
        public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

        public String getPrimerNombre() { return primerNombre; }
        public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

        public String getSegundoNombre() { return segundoNombre; }
        public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

        public String getPrimerApellido() { return primerApellido; }
        public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

        public String getSegundoApellido() { return segundoApellido; }
        public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDate getFechaNacimiento() { return fechaNacimiento; }
        public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

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

    public static class SuccessResponse {
        private String message;

        public SuccessResponse() {}
        public SuccessResponse(String message) { this.message = message; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
