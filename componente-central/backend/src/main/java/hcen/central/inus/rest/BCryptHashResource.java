package hcen.central.inus.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Endpoint TEMPORAL para generar hashes BCrypt
 * ELIMINAR DESPUÉS DE ACTUALIZAR LA CONTRASEÑA EN BD
 */
@Path("/hash")
@Produces(MediaType.APPLICATION_JSON)
public class BCryptHashResource {

    @GET
    @Path("/generate/{password}")
    public Response generateHash(@PathParam("password") String password) {
        // Generar hash con 12 rounds (igual que authentication_service)
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // Verificar que el hash funciona
        boolean matches = BCrypt.checkpw(password, hash);

        return Response.ok()
                .entity("{\n" +
                        "  \"password\": \"" + password + "\",\n" +
                        "  \"hash\": \"" + hash + "\",\n" +
                        "  \"verification\": " + matches + ",\n" +
                        "  \"sql\": \"UPDATE admin_hcen SET passwordhash = '" + hash + "' WHERE username = 'admin';\"\n" +
                        "}")
                .build();
    }

    @GET
    @Path("/verify/{password}/{hash}")
    public Response verifyHash(@PathParam("password") String password, @PathParam("hash") String hash) {
        boolean matches = BCrypt.checkpw(password, hash);

        return Response.ok()
                .entity("{\n" +
                        "  \"password\": \"" + password + "\",\n" +
                        "  \"hash\": \"" + hash + "\",\n" +
                        "  \"matches\": " + matches + "\n" +
                        "}")
                .build();
    }
}