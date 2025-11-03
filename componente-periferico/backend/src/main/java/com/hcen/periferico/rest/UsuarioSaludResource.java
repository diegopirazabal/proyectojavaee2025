package com.hcen.periferico.rest;

import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.service.UsuarioSaludService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Resource para gestión de usuarios de salud en componente periférico.
 *
 * ARQUITECTURA NUEVA:
 * - Consulta BD local del periférico (no delega al central para consultas)
 * - tenant_id debe obtenerse del contexto del admin logueado (JWT/sesión)
 * - Registros se persisten localmente y se sincronizan con central
 *
 * Base path: /usuarios
 */
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioSaludResource {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludResource.class.getName());

    @EJB
    private UsuarioSaludService usuarioService;

    @Context
    private SecurityContext securityContext;

    /**
     * TEMPORAL: Extrae tenant_id del request o de la sesión
     * TODO: Implementar extracción desde JWT cuando se integre autenticación
     */
    private UUID extractTenantId(String tenantIdFromRequest) {
        // Por ahora usamos el tenantId del request
        // FUTURO: Extraer desde JWT del admin logueado
        // UUID tenantId = getTenantIdFromJWT(securityContext);

        if (tenantIdFromRequest == null || tenantIdFromRequest.trim().isEmpty()) {
            throw new IllegalArgumentException("tenant_id es requerido (futuro: se obtendrá del JWT)");
        }
        return UUID.fromString(tenantIdFromRequest);
    }

    /**
     * Lista todos los usuarios de una clínica o busca por término (consulta BD local)
     * GET /usuarios?tenantId={uuid}&search={term}
     */
    @GET
    public Response getUsuarios(@QueryParam("tenantId") String tenantIdStr,
                                @QueryParam("search") String searchTerm) {
        try {
            UUID tenantId = extractTenantId(tenantIdStr);

            java.util.List<usuario_salud_dto> usuarios;

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                // Búsqueda con filtro en BD local
                usuarios = usuarioService.searchUsuariosByTenantId(searchTerm, tenantId);
            } else {
                // Listar todos desde BD local
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
     * Obtiene un usuario por cédula y tenant (desde BD local)
     * GET /usuarios/{cedula}?tenantId={uuid}
     */
    @GET
    @Path("/{cedula}")
    public Response getUsuarioByCedula(@PathParam("cedula") String cedula,
                                       @QueryParam("tenantId") String tenantIdStr) {
        try {
            UUID tenantId = extractTenantId(tenantIdStr);

            usuario_salud_dto usuario = usuarioService.getUsuarioByCedulaAndTenant(cedula, tenantId);
            if (usuario != null) {
                return Response.ok(usuario).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Usuario no encontrado en esta clínica"))
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
     * Registra un usuario en una clínica (persiste en BD local + sync con central)
     * POST /usuarios/registrar
     */
    @POST
    @Path("/registrar")
    public Response registrarUsuario(RegistrarUsuarioRequest request) {
        try {
            // Extraer tenant_id (por ahora del request, futuro: del JWT)
            UUID tenantId = extractTenantId(request.getTenantId());

            // Convertir tipoDocumento String a enum
            TipoDocumento tipoDoc = request.getTipoDocumento() != null && !request.getTipoDocumento().isEmpty()
                ? TipoDocumento.valueOf(request.getTipoDocumento())
                : TipoDocumento.DO;

            // Registrar localmente y sincronizar con central
            usuario_salud_dto usuario = usuarioService.registrarUsuarioEnClinica(
                request.getCedula(),
                tipoDoc,
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getEmail(),
                request.getFechaNacimiento(),
                tenantId
            );

            LOGGER.info("Usuario registrado localmente: " + request.getCedula());
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
     * Desactiva un usuario de una clínica (soft delete en BD local)
     * DELETE /usuarios/{cedula}/clinica/{tenantId}
     */
    @DELETE
    @Path("/{cedula}/clinica/{tenantId}")
    public Response deleteUsuario(@PathParam("cedula") String cedula,
                                  @PathParam("tenantId") String tenantIdStr) {
        try {
            UUID tenantId = extractTenantId(tenantIdStr);

            usuarioService.desactivarUsuario(cedula, tenantId);

            return Response.ok(new SuccessResponse("Usuario desactivado de la clínica exitosamente")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al desactivar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al desactivar usuario: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Obtiene usuarios pendientes de sincronización con central
     * GET /usuarios/pendientes-sync?tenantId={uuid}
     */
    @GET
    @Path("/pendientes-sync")
    public Response getUsuariosPendientesSincronizacion(@QueryParam("tenantId") String tenantIdStr) {
        try {
            UUID tenantId = extractTenantId(tenantIdStr);

            java.util.List<usuario_salud_dto> pendientes =
                usuarioService.getUsuariosPendientesSincronizacion(tenantId);

            return Response.ok(pendientes).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios pendientes", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener usuarios pendientes: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Reintenta sincronización de usuarios pendientes
     * POST /usuarios/reintentar-sync?tenantId={uuid}
     */
    @POST
    @Path("/reintentar-sync")
    public Response reintentarSincronizacion(@QueryParam("tenantId") String tenantIdStr) {
        try {
            UUID tenantId = extractTenantId(tenantIdStr);

            usuarioService.reintentarSincronizacionesPendientes(tenantId);

            return Response.ok(new SuccessResponse("Sincronización reiniciada para usuarios pendientes")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al reintentar sincronización", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al reintentar sincronización: " + e.getMessage()))
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

        @jakarta.json.bind.annotation.JsonbTypeAdapter(LocalDateAdapter.class)
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
