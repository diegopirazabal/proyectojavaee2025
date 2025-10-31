package hcen.central.inus.api;

import hcen.central.inus.dto.UsuarioSistemaResponse;
import hcen.central.inus.service.UsuarioSistemaService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Path("/usuarios-sistema")
@Produces(MediaType.APPLICATION_JSON)
public class UsuariosSistemaResource {

    private static final Logger LOGGER = Logger.getLogger(UsuariosSistemaResource.class.getName());

    @EJB
    private UsuarioSistemaService usuarioSistemaService;

    @GET
    public Response listarUsuarios(@QueryParam("tipoDoc") String tipoDocumento,
                                   @QueryParam("numeroDoc") String numeroDocumento,
                                   @QueryParam("nombre") String nombre,
                                   @QueryParam("apellido") String apellido,
                                   @QueryParam("limit") Integer limit) {
        try {
            List<UsuarioSistemaResponse> usuarios = usuarioSistemaService.obtenerCatalogo(
                tipoDocumento,
                numeroDocumento,
                nombre,
                apellido,
                limit
            );
            return Response.ok(usuarios).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al listar usuarios del sistema", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener usuarios del sistema"))
                .build();
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
