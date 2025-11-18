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
 * REST Resource para gestión de usuarios de salud en el sistema nacional
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
     * Registra un usuario en el sistema nacional
     * Si el usuario ya existe por cédula: devuelve el existente (status OK)
     * Si no existe: crea un nuevo usuario con datos mínimos
     * POST /api/usuarios/registrar
     */
    @POST
    @Path("/registrar")
    public Response registrarUsuario(RegistrarUsuarioRequest request) {
        try {
            LOGGER.info("Recibida solicitud de registro para cédula: " +
                (request != null ? request.getCedula() : "null"));
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

}
