package hcen.central.inus.api;

import hcen.central.inus.dao.OIDCUserDAO;
import hcen.central.inus.dto.UsuarioSaludResponse;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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

    @GET
    public Response listarUsuariosActivos() {
        List<UsuarioSaludResponse> usuarios = oidcUserDAO.findAll()
                .stream()
                .map(UsuarioSaludResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(usuarios).build();
    }
}
