package hcen.central.inus.rest;

import hcen.central.inus.dao.OIDCSessionDAO;
import hcen.central.inus.dao.OIDCUserDAO;
import hcen.central.inus.dto.JWTTokenResponse;
import hcen.central.inus.dto.OIDCAuthRequest;
import hcen.central.inus.dto.OIDCTokenResponse;
import hcen.central.inus.dto.OIDCUserInfo;
import hcen.central.inus.entity.OIDCSession;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.security.oidc.OIDCAuthenticationService;
import hcen.central.inus.security.oidc.OIDCCallbackHandler;
import hcen.central.inus.security.oidc.OIDCUserInfoService;
import io.jsonwebtoken.Claims;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Resource para endpoints de autenticación OIDC
 * Base path: /api/auth
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OIDCAuthResource {

    private static final Logger LOGGER = Logger.getLogger(OIDCAuthResource.class.getName());

    @Inject
    private OIDCAuthenticationService authService;

    @Inject
    private OIDCCallbackHandler callbackHandler;

    // TODO: QUITAR LUEGO, ES SOLO PARA DESARROLLO - Inicio
    @Inject
    private OIDCUserInfoService userInfoService;

    @Inject
    private OIDCUserDAO userDAO;

    @Inject
    private OIDCSessionDAO sessionDAO;
    // TODO: QUITAR LUEGO, ES SOLO PARA DESARROLLO - Fin

    @Context
    private HttpServletRequest httpRequest;

    /**
     * GET /api/auth/login
     * Inicia el flujo de autenticación OIDC
     * Redirige al usuario al authorization endpoint de gub.uy
     *
     * @param redirectUri URI de redirección (debe estar registrada en gub.uy)
     * @return Redirección 302 al authorization endpoint
     */
    @GET
    @Path("/login")
    public Response login(@QueryParam("redirect_uri") String redirectUri) {
        try {
            LOGGER.info("··· Iniciando login OIDC");

            // Validar redirect_uri
            if (redirectUri == null || redirectUri.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("redirect_uri es requerido"))
                        .build();
            }

            // Generar request de autorización
            OIDCAuthRequest authRequest = authService.initiateLogin(redirectUri);

            // Guardar state, nonce y code_verifier en la sesión HTTP
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("oidc_state", authRequest.getState());
            session.setAttribute("oidc_nonce", authRequest.getNonce());
            session.setAttribute("oidc_code_verifier", authRequest.getCodeVerifier());
            session.setAttribute("oidc_redirect_uri", redirectUri);

            LOGGER.info("Redirigiendo al authorization endpoint de gub.uy");

            // Redirigir al authorization endpoint
            return Response.seeOther(URI.create(authRequest.getAuthorizationUrl())).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en login OIDC", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Error iniciando autenticación: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/auth/callback
     * Maneja el callback de gub.uy después de la autenticación
     * Intercambia el authorization code por tokens y genera JWT propios
     *
     * @param code  Authorization code
     * @param state State parameter (protección CSRF)
     * @param error Error code (si la autenticación falló)
     * @param errorDescription Descripción del error
     * @return JWTTokenResponse con los tokens de la aplicación
     */
    @GET
    @Path("/callback")
    public Response callback(
            @QueryParam("code") String code,
            @QueryParam("state") String state,
            @QueryParam("error") String error,
            @QueryParam("error_description") String errorDescription) {
        try {
            LOGGER.info("··· Procesando callback OIDC");

            // Verificar si hay error
            if (error != null) {
                LOGGER.log(Level.SEVERE, "Error en callback: " + error + " - " + errorDescription);
                callbackHandler.handleError(error, errorDescription);
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(createErrorResponse("Autenticación fallida: " + error))
                        .build();
            }

            // Validar code y state
            if (code == null || state == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("code y state son requeridos"))
                        .build();
            }

            // Recuperar datos de la sesión
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(createErrorResponse("Sesión expirada"))
                        .build();
            }

            String expectedState = (String) session.getAttribute("oidc_state");
            String expectedNonce = (String) session.getAttribute("oidc_nonce");
            String codeVerifier = (String) session.getAttribute("oidc_code_verifier");
            String redirectUri = (String) session.getAttribute("oidc_redirect_uri");

            // Limpiar atributos de sesión
            session.removeAttribute("oidc_state");
            session.removeAttribute("oidc_nonce");
            session.removeAttribute("oidc_code_verifier");
            session.removeAttribute("oidc_redirect_uri");

            // Procesar callback y obtener tokens
            JWTTokenResponse tokenResponse = authService.handleCallback(
                    code, state, expectedState, expectedNonce, codeVerifier, redirectUri
            );

            LOGGER.info("Éxito en callback OIDC");

            // TODO: QUITAR LUEGO, ES SOLO PARA DESARROLLO - Inicio
            // Guardar datos en sesión HTTP para el frontend JSF de desarrollo
            try {
                String userSub = tokenResponse.getUserSub();
                
                // Obtener usuario de la BD
                UsuarioSalud user = userDAO.findBySub(userSub);
                
                // Obtener sesión OIDC de la BD (la más reciente)
                List<OIDCSession> sessions = sessionDAO.findByUserSub(userSub);
                OIDCSession oidcSession = sessions.isEmpty() ? null : sessions.get(0);
                
                if (user != null && oidcSession != null) {
                    // Crear DTO de UserInfo desde entidad
                    OIDCUserInfo userInfo = new OIDCUserInfo();
                    userInfo.setSub(user.getSub());
                    userInfo.setEmail(user.getEmail());
                    userInfo.setEmailVerified(user.isEmailVerified());
                    userInfo.setFullName(user.getFullName());
                    userInfo.setFirstName(user.getFirstName());
                    userInfo.setLastName(user.getLastName());
                    userInfo.setDocumentType(user.getDocumentType());
                    userInfo.setDocumentNumber(user.getDocumentNumber());
                    userInfo.setUid(user.getUid());
                    userInfo.setRid(user.getRid());
                    userInfo.setNid(user.getNid());
                    
                    // Guardar en sesión HTTP
                    session.setAttribute("userInfo", userInfo);
                    session.setAttribute("jwtToken", tokenResponse);
                    session.setAttribute("rawIdToken", oidcSession.getIdToken());
                    session.setAttribute("sessionId", oidcSession.getSessionId());
                    session.setAttribute("sessionCreated", java.util.Date.from(oidcSession.getCreatedAt()));
                    
                    LOGGER.info("Datos guardados en sesión HTTP para desarrollo");
                    
                    // Redirigir al dashboard JSF
                    return Response.seeOther(URI.create(httpRequest.getContextPath() + "/dashboard.xhtml")).build();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error guardando datos en sesión para desarrollo", e);
            }
            // TODO: QUITAR LUEGO, ES SOLO PARA DESARROLLO - Fin

            return Response.ok(tokenResponse).build();

        } catch (SecurityException e) {
            LOGGER.log(Level.SEVERE, "Error de seguridad en callback", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(createErrorResponse("Error de seguridad: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en callback OIDC", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Error procesando callback: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * POST /api/auth/refresh
     * Refresca un JWT usando el refresh token
     *
     * Request body: { "refresh_token": "..." }
     *
     * @param requestBody Cuerpo de la petición con refresh_token
     * @return Nuevo JWTTokenResponse
     */
    @POST
    @Path("/refresh")
    public Response refresh(Map<String, String> requestBody) {
        try {
            LOGGER.info("··· Refrescando JWT");

            String refreshToken = requestBody.get("refresh_token");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("refresh_token es requerido"))
                        .build();
            }

            JWTTokenResponse tokenResponse = authService.refreshToken(refreshToken);

            LOGGER.info("Éxito al refrescar JWT");
            return Response.ok(tokenResponse).build();

        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Refresh token inválido", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(createErrorResponse("Refresh token inválido"))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refrescando JWT", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Error refrescando token: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * POST /api/auth/logout
     * Cierra la sesión del usuario
     *
     * Requiere header: Authorization: Bearer <access_token>
     *
     * @param authHeader Header de autorización con JWT
     * @return Respuesta de éxito
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        try {
            LOGGER.info("··· Cerrando sesión");

            // Extraer token del header
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(createErrorResponse("Token no proporcionado"))
                        .build();
            }

            // Validar y extraer subject del token
            // Nota: Aquí deberías inyectar JWTTokenProvider para validar el token
            // Por simplicidad, asumimos que el filtro JWT ya validó el token
            // y puedes extraer el subject directamente

            // TODO: Extraer userSub del token JWT validado
            // Claims claims = jwtTokenProvider.validateAccessToken(token);
            // String userSub = claims.getSubject();

            // Por ahora, devolvemos éxito genérico
            // authService.logout(userSub);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sesión cerrada exitosamente");

            LOGGER.info("Éxito al cerrar sesión");
            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en logout", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Error cerrando sesión: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/auth/me
     * Obtiene información del usuario autenticado actual
     *
     * Requiere header: Authorization: Bearer <access_token>
     *
     * @param authHeader Header de autorización con JWT
     * @return Información del usuario
     */
    @GET
    @Path("/me")
    public Response me(@HeaderParam("Authorization") String authHeader) {
        try {
            LOGGER.info("··· Obteniendo info del usuario autenticado");

            // Extraer token del header
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(createErrorResponse("Token no proporcionado"))
                        .build();
            }

            // TODO: Validar token y extraer userSub
            // Claims claims = jwtTokenProvider.validateAccessToken(token);
            // String userSub = claims.getSubject();
            // UsuarioSalud user = userDAO.findBySub(userSub);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Endpoint /me - TODO: Implementar validación JWT y retornar info del usuario");

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo info del usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Extrae el token del header Authorization
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Crea una respuesta de error estándar
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        return error;
    }
}
