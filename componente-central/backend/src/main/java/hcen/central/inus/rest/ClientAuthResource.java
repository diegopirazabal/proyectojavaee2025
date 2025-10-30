package hcen.central.inus.rest;

import hcen.central.inus.dto.ClientAuthRequest;
import hcen.central.inus.dto.ClientAuthResponse;
import hcen.central.inus.service.ClientAuthenticationService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

/**
 * REST Resource para autenticación de clientes externos (componente-periferico)
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientAuthResource {
    
    private static final Logger LOGGER = Logger.getLogger(ClientAuthResource.class.getName());
    
    @EJB
    private ClientAuthenticationService clientAuthService;
    
    /**
     * Endpoint para obtener JWT usando client_credentials
     * POST /api/auth/token
     */
    @POST
    @Path("/token")
    public Response getToken(ClientAuthRequest request) {
        try {
            LOGGER.info("Recibida petición de autenticación para cliente: " + request.getClientId());
            
            // Validar request
            if (request.getClientId() == null || request.getClientSecret() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("client_id y client_secret son requeridos"))
                    .build();
            }
            
            // Autenticar y generar JWT
            String jwt = clientAuthService.authenticateClient(
                request.getClientId(), 
                request.getClientSecret()
            );
            
            if (jwt == null) {
                LOGGER.warning("Autenticación fallida para cliente: " + request.getClientId());
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Credenciales inválidas"))
                    .build();
            }
            
            // Construir response
            long expiresIn = clientAuthService.getTokenExpirationSeconds();
            ClientAuthResponse response = new ClientAuthResponse(jwt, expiresIn);
            
            LOGGER.info("Token generado exitosamente para cliente: " + request.getClientId());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.severe("Error al procesar autenticación: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error interno del servidor"))
                .build();
        }
    }
    
    /**
     * Clase interna para responses de error
     */
    public static class ErrorResponse {
        private String error;
        
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
