package hcen.central.inus.rest;

import hcen.central.inus.dao.OIDCUserDAO;
import hcen.central.inus.dto.JWTTokenResponse;
import hcen.central.inus.dto.OIDCAuthRequest;
import hcen.central.inus.dto.OIDCTokenResponse;
import hcen.central.inus.dto.OIDCUserInfo;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.security.oidc.OIDCAuthenticationService;
import hcen.central.inus.security.oidc.OIDCCallbackHandler;
import hcen.central.inus.security.oidc.OIDCUserInfoService;
import io.jsonwebtoken.Claims;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
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
    public Response login(
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("origin") String origin) {
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

            // Guardar state y nonce en la sesión HTTP (PKCE removido)
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("oidc_state", authRequest.getState());
            session.setAttribute("oidc_nonce", authRequest.getNonce());
            // session.setAttribute("oidc_code_verifier", authRequest.getCodeVerifier()); // PKCE removido
            // session.setAttribute("oidc_code_challenge", authRequest.getCodeChallenge()); // PKCE removido
            session.setAttribute("oidc_redirect_uri", redirectUri);
            
            // Detectar origen del login desde parámetro origin (prioritario) o Referer header
            String loginOrigin = origin; // Usar el parámetro origin si viene
            
            if (loginOrigin == null || loginOrigin.isBlank()) {
                // Fallback: detectar desde Referer header
                String referer = httpRequest.getHeader("Referer");
                loginOrigin = "admin"; // Por defecto admin
                
                if (referer != null) {
                    if (referer.contains("portal-salud") || referer.contains("portal-usuario")) {
                        loginOrigin = "usuario-salud";
                    } else if (referer.contains("portal-admin") || referer.contains("frontend-admin-hcen")) {
                        loginOrigin = "admin";
                    }
                }
            }
            
            session.setAttribute("oidc_login_origin", loginOrigin);
            LOGGER.info("Login origin detectado: '" + loginOrigin + "' (origin param: '" + origin + "')");
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
            // String codeVerifier = (String) session.getAttribute("oidc_code_verifier"); // PKCE removido
            String redirectUri = (String) session.getAttribute("oidc_redirect_uri");

            // Limpiar atributos de sesión
            session.removeAttribute("oidc_state");
            session.removeAttribute("oidc_nonce");
            // session.removeAttribute("oidc_code_verifier"); // PKCE removido
            session.removeAttribute("oidc_redirect_uri");

            // Procesar callback y obtener tokens (sin PKCE)
            JWTTokenResponse tokenResponse = authService.handleCallback(
                    code, state, expectedState, expectedNonce, redirectUri
            );

            LOGGER.info("Éxito en callback OIDC");

            // TODO: QUITAR LUEGO, ES SOLO PARA DESARROLLO - Inicio
            // Guardar datos en sesión HTTP y cookie HttpOnly para el frontend JSF de desarrollo
            try {
                String cedula = tokenResponse.getUserSub();
                
                // Obtener usuario de la BD
                UsuarioSalud user = userDAO.findByCedula(cedula);
                
                if (user != null) {
                    // Crear DTO de UserInfo desde entidad
                    OIDCUserInfo userInfo = new OIDCUserInfo();
                    userInfo.setNumeroDocumento(user.getCedula());
                    userInfo.setEmail(user.getEmail());
                    userInfo.setEmailVerified(user.isEmailVerificado());
                    userInfo.setNombreCompleto(user.getNombreCompleto());
                    userInfo.setPrimerNombre(user.getPrimerNombre());
                    userInfo.setSegundoNombre(user.getSegundoNombre());
                    userInfo.setPrimerApellido(user.getPrimerApellido());
                    userInfo.setSegundoApellido(user.getSegundoApellido());
                    
                    // Guardar en sesión HTTP
                    session.setAttribute("userInfo", userInfo);
                    session.setAttribute("jwtToken", tokenResponse);
                    
                    LOGGER.info("Datos guardados en sesión HTTP y cookie para desarrollo");
                    
                    // Determinar dashboard según origen del login y entorno
                    String loginOrigin = (String) session.getAttribute("oidc_login_origin");
                    String serverName = httpRequest.getServerName();
                    boolean isProduction = "hcen-uy.web.elasticloud.uy".equals(serverName);
                    
                    LOGGER.info("Callback - loginOrigin: '" + loginOrigin + "', serverName: '" + serverName + "', isProduction: " + isProduction);
                    
                    // Si el origen es mobile, redirigir al deep link de la app
                    if ("mobile".equals(loginOrigin)) {
                        LOGGER.info("Redirigiendo a aplicación móvil");
                        
                        // Construir deep link para la app móvil
                        StringBuilder mobileCallbackUrl = new StringBuilder("hcenmobile://auth/callback");
                        mobileCallbackUrl.append("?jwt_token=")
                                        .append(java.net.URLEncoder.encode(tokenResponse.getAccessToken(), java.nio.charset.StandardCharsets.UTF_8))
                                        .append("&cedula=")
                                        .append(java.net.URLEncoder.encode(cedula, java.nio.charset.StandardCharsets.UTF_8));
                        
                        // Agregar nombre completo si está disponible
                        if (user.getNombreCompleto() != null && !user.getNombreCompleto().isBlank()) {
                            mobileCallbackUrl.append("&nombre_completo=")
                                           .append(java.net.URLEncoder.encode(user.getNombreCompleto(), java.nio.charset.StandardCharsets.UTF_8));
                        }
                        
                        // Limpiar atributo de origen
                        session.removeAttribute("oidc_login_origin");
                        
                        // Redirigir al deep link de la app
                        return Response.seeOther(URI.create(mobileCallbackUrl.toString()))
                                .build();
                    }
                    
                    // Flujo normal para web (admin o usuario-salud)
                    String contextPath;
                    
                    if ("usuario-salud".equals(loginOrigin)) {
                        contextPath = isProduction ? "/portal-usuario" : "/portal-salud";
                    } else {
                        // Admin: /portal-admin en producción, /frontend-admin-hcen en desarrollo
                        contextPath = isProduction ? "/portal-admin" : "/frontend-admin-hcen";
                    }
                    
                    LOGGER.info("Redirigiendo a contextPath: '" + contextPath + "'");
                    
                    // Construir URL del dashboard
                    String scheme = httpRequest.getScheme();
                    int serverPort = httpRequest.getServerPort();
                    
                    StringBuilder dashboardUrl = new StringBuilder(scheme).append("://").append(serverName);
                    if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
                        dashboardUrl.append(":").append(serverPort);
                    }
                    dashboardUrl.append(contextPath).append("/dashboard.xhtml");
                    
                    // Agregar cédula como parámetro si es portal usuario-salud
                    if ("usuario-salud".equals(loginOrigin)) {
                        dashboardUrl.append("?docType=DO&docNumber=")
                                   .append(java.net.URLEncoder.encode(cedula, java.nio.charset.StandardCharsets.UTF_8));
                    }
                    
                    // Limpiar atributo de origen
                    session.removeAttribute("oidc_login_origin");
                    
                    // Redirigir al dashboard JSF con cookie HttpOnly
                    return Response.seeOther(URI.create(dashboardUrl.toString()))
                            .cookie(createJwtCookie(tokenResponse.getAccessToken(), tokenResponse.getExpiresIn()))
                            .build();
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
     * Cierra la sesión del usuario y borra la cookie JWT
     *
     * Requiere header: Authorization: Bearer <access_token> o cookie jwt_token
     *
     * @param authHeader Header de autorización con JWT (opcional si hay cookie)
     * @return Respuesta de éxito con cookie borrada
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        try {
            LOGGER.info("··· Cerrando sesión");

            // Extraer token del header (validación opcional, el filtro ya lo hizo)
            String token = extractTokenFromHeader(authHeader);
            
            // TODO: Si se implementa invalidación de tokens (blacklist), hacerlo aquí
            // authService.invalidateToken(token);

            // Invalidar sesión HTTP si existe
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
                LOGGER.info("Sesión HTTP invalidada");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sesión cerrada exitosamente");

            LOGGER.info("Éxito al cerrar sesión");
            
            // Retornar respuesta con cookie JWT borrada
            return Response.ok(response)
                    .cookie(deleteJwtCookie())
                    .build();

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

    /**
     * Crea una cookie HttpOnly para el JWT access token
     * 
     * @param accessToken Token JWT
     * @param expiresInSeconds Duración del token en segundos
     * @return NewCookie para JAX-RS Response
     */
    private NewCookie createJwtCookie(String accessToken, long expiresInSeconds) {
        return new NewCookie.Builder("jwt_token")
                .value(accessToken)
                .path("/")
                .maxAge((int) expiresInSeconds)
                .httpOnly(true)
                .secure(false) // TODO: Cambiar a true en producción con HTTPS
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }

    /**
     * Crea una cookie vacía para borrar el JWT (usado en logout)
     * 
     * @return NewCookie con maxAge=0 para borrar la cookie
     */
    private NewCookie deleteJwtCookie() {
        return new NewCookie.Builder("jwt_token")
                .value("")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false) // TODO: Cambiar a true en producción con HTTPS
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }
}
