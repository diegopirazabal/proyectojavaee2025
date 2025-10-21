package hcen.central.inus.security.filter;

import hcen.central.inus.security.jwt.JWTTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filtro para validar JWT propios del sistema en requests
 * Se ejecuta ANTES de los recursos REST protegidos
 * 
 * Flujo:
 * 1. Extrae token del header Authorization: Bearer <token>
 * 2. Valida token usando JWTTokenProvider
 * 3. Si válido: permite acceso y setea contexto de seguridad
 * 4. Si inválido: retorna 401 Unauthorized
 */
@WebFilter(urlPatterns = {"/api/*"})
public class JWTAuthenticationFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(JWTAuthenticationFilter.class.getName());

    @Inject
    private JWTTokenProvider jwtTokenProvider;

    // Rutas públicas que NO requieren autenticación
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/callback",
            "/api/auth/refresh",
            "/api/fcm/register",              // Registro de token FCM desde mobile
            "/api/fcm/unregister",            // Eliminación de token FCM
            "/api/notifications/broadcast-test",  // Envío de notificación de prueba desde AdminHCEN
            "/api/usuarios-salud"             // Listado de usuarios (usado por AdminHCEN)
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("JWTAuthenticationFilter inicializado");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        LOGGER.fine("Procesando request: " + method + " " + requestURI);

        // Permitir acceso a rutas públicas sin autenticación
        if (isPublicPath(requestURI)) {
            LOGGER.fine("Ruta pública, permitiendo acceso sin autenticación: " + requestURI);
            chain.doFilter(request, response);
            return;
        }

        // Permitir solicitudes OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            LOGGER.fine("Request OPTIONS (CORS preflight), permitiendo acceso");
            chain.doFilter(request, response);
            return;
        }

        try {
            // Extraer token del header Authorization
            String token = extractTokenFromHeader(httpRequest);

            if (token == null || token.isEmpty()) {
                LOGGER.warning("Token no proporcionado en header Authorization para: " + requestURI);
                sendUnauthorizedResponse(httpResponse, "Token no proporcionado");
                return;
            }

            // Validar token
            Claims claims = jwtTokenProvider.validateAccessToken(token);

            // Extraer información del token
            String userSub = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            LOGGER.info("Token válido para usuario: " + userSub + ", roles: " + roles);

            // Setear atributos en el request para uso posterior
            httpRequest.setAttribute("userSub", userSub);
            httpRequest.setAttribute("userRoles", roles);
            httpRequest.setAttribute("jwtClaims", claims);

            // Continuar con la cadena de filtros
            chain.doFilter(request, response);

        } catch (JwtException e) {
            LOGGER.log(Level.WARNING, "Token JWT inválido: " + e.getMessage(), e);
            sendUnauthorizedResponse(httpResponse, "Token inválido: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en filtro JWT", e);
            sendUnauthorizedResponse(httpResponse, "Error de autenticación");
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("JWTAuthenticationFilter destruido");
    }

    /**
     * Extrae el token JWT del header Authorization
     *
     * @param request HttpServletRequest
     * @return Token JWT o null si no está presente
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remover "Bearer " prefix
        }

        return null;
    }

    /**
     * Verifica si la ruta es pública (no requiere autenticación)
     *
     * @param requestURI URI del request
     * @return true si es ruta pública
     */
    private boolean isPublicPath(String requestURI) {
        for (String publicPath : PUBLIC_PATHS) {
            if (requestURI.endsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Envía respuesta 401 Unauthorized
     *
     * @param response HttpServletResponse
     * @param message  Mensaje de error
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"error\": true, \"message\": \"%s\"}",
                message.replace("\"", "\\\"")
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
