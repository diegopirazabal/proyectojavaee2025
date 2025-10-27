package hcen.central.inus.rest;

import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.service.UsuarioSaludService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Resource para gestión de usuarios de salud desde clínicas periféricas
 * Base path: /api/usuarios
 */
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioSaludResource {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludResource.class.getName());

    @EJB
    private UsuarioSaludService usuarioService;

    /**
     * Lista todos los usuarios de una clínica
     * GET /api/usuarios?tenantId={uuid}
     */
    @GET
    public Response getUsuariosByTenantId(@QueryParam("tenantId") String tenantIdStr,
                                          @QueryParam("search") String searchTerm) {
        try {
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El parámetro tenantId es requerido"))
                    .build();
            }

            java.util.UUID tenantId = java.util.UUID.fromString(tenantIdStr);
            java.util.List<UsuarioSaludDTO> usuarios;

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                // Búsqueda con filtro
                usuarios = usuarioService.searchUsuariosByTenantId(searchTerm, tenantId);
            } else {
                // Listar todos
                usuarios = usuarioService.getUsuariosByTenantId(tenantId);
            }

            return Response.ok(usuarios).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error interno del servidor"))
                .build();
        }
    }

    /**
     * Verifica si un usuario existe por cédula
     * GET /api/usuarios/verificar/{cedula}
     */
    @GET
    @Path("/verificar/{cedula}")
    public Response verificarUsuarioExiste(@PathParam("cedula") String cedula) {
        try {
            boolean existe = usuarioService.verificarUsuarioExiste(cedula);
            return Response.ok(Map.of("existe", existe, "cedula", cedula)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error interno del servidor"))
                .build();
        }
    }

    /**
     * Registra un usuario en una clínica
     * Si el usuario existe, actualiza sus datos y crea la asociación
     * Si no existe, lo crea y crea la asociación
     * POST /api/usuarios/registrar
     */
    @POST
    @Path("/registrar")
    public Response registrarUsuario(RegistrarUsuarioRequest request) {
        try {
            LOGGER.info("Recibida solicitud de registro: " + request);
            UsuarioSaludDTO usuarioDTO = usuarioService.registrarUsuarioEnClinica(request);
            return Response.ok(usuarioDTO).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validación fallida: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error interno del servidor: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Obtiene datos de un usuario por cédula
     * GET /api/usuarios/{cedula}
     */
    @GET
    @Path("/{cedula}")
    public Response getUsuarioByCedula(@PathParam("cedula") String cedula) {
        try {
            Optional<UsuarioSaludDTO> usuario = usuarioService.getUsuarioByCedula(cedula);
            if (usuario.isPresent()) {
                return Response.ok(usuario.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Usuario no encontrado", "cedula", cedula))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error interno del servidor"))
                .build();
        }
    }

    /**
     * Desasocia un usuario de una clínica (elimina el registro con esa combinación cedula+tenant_id)
     * DELETE /api/usuarios/{cedula}/clinica/{tenantId}
     */
    @DELETE
    @Path("/{cedula}/clinica/{tenantId}")
    public Response desasociarUsuarioDeClinica(@PathParam("cedula") String cedula,
                                               @PathParam("tenantId") String tenantIdStr) {
        try {
            java.util.UUID tenantId = java.util.UUID.fromString(tenantIdStr);
            boolean deleted = usuarioService.desasociarUsuarioDeClinica(cedula, tenantId);
            if (deleted) {
                return Response.ok(Map.of("message", "Usuario desasociado de la clínica exitosamente")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No se encontró el usuario en esa clínica"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al desasociar usuario de clínica", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error interno del servidor: " + e.getMessage()))
                .build();
        }
    }
}
