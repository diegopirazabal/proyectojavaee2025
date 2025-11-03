package hcen.central.inus.api;

import hcen.central.inus.dao.OIDCUserDAO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.dto.UsuarioSaludResponse;
import hcen.central.inus.service.UsuarioSaludService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Path("/usuarios-salud")
@Produces(MediaType.APPLICATION_JSON)
public class usuario_salud_resource {

    @EJB
    private OIDCUserDAO oidcUserDAO;

    @EJB
    private UsuarioSaludService usuarioSaludService;

    @GET
    public Response listarUsuariosActivos() {
        List<UsuarioSaludResponse> usuarios = oidcUserDAO.findAll()
                .stream()
                .map(UsuarioSaludResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(usuarios).build();
    }

    @PUT
    @Path("/{cedula}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizarUsuario(@PathParam("cedula") String cedula,
                                      ActualizarUsuarioSaludRequest request) {
        try {
            UsuarioSaludDTO actualizado = usuarioSaludService.actualizarUsuario(cedula, request);
            return Response.ok(actualizado).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("No se pudo actualizar el usuario"))
                    .build();
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}

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
