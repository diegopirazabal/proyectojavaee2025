package hcen.central.inus.service;

import hcen.central.inus.dao.JWTSessionDAO;
import hcen.central.inus.entity.JWTSession;
import hcen.central.inus.security.jwt.JWTConfiguration;
import hcen.central.inus.security.jwt.JWTTokenProvider;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servicio para autenticar clientes externos (componente-periferico)
 * usando client_credentials flow
 */
@Stateless
public class ClientAuthenticationService {
    
    private static final Logger LOGGER = Logger.getLogger(ClientAuthenticationService.class.getName());
    
    // Credenciales hardcodeadas en componente-central para validar
    private static final String VALID_CLIENT_ID = "componente-periferico";
    private static final String VALID_CLIENT_SECRET = "hcen2025_periferico_secret_key";
    
    @EJB
    private JWTTokenProvider jwtTokenProvider;
    
    @EJB
    private JWTSessionDAO jwtSessionDAO;
    
    @EJB
    private JWTConfiguration jwtConfig;
    
    /**
     * Autentica un cliente y genera un JWT
     * @param clientId ID del cliente
     * @param clientSecret Secret del cliente
     * @return JWT token o null si las credenciales son inválidas
     */
    public String authenticateClient(String clientId, String clientSecret) {
        LOGGER.info("Intentando autenticar cliente: " + clientId);
        
        // Validar credenciales
        if (!isValidClient(clientId, clientSecret)) {
            LOGGER.warning("Credenciales inválidas para cliente: " + clientId);
            return null;
        }
        
        LOGGER.info("Credenciales válidas para cliente: " + clientId);
        
        // Buscar si ya existe un token activo y válido para este cliente
        List<JWTSession> activeSessions = jwtSessionDAO.findActiveByClientId(clientId);
        
        if (!activeSessions.isEmpty()) {
            JWTSession existingSession = activeSessions.get(0);
            
            // Verificar si el token aún no ha expirado
            if (!existingSession.isExpired()) {
                LOGGER.info("Reutilizando token activo existente para cliente: " + clientId);
                // Actualizar último uso
                jwtSessionDAO.updateLastUsed(existingSession.getId());
                return existingSession.getJwtToken();
            } else {
                // Token expirado, invalidarlo
                LOGGER.info("Token existente expirado, invalidando y generando nuevo");
                jwtSessionDAO.invalidateByToken(existingSession.getJwtToken());
            }
        }
        
        // Generar nuevo JWT con rol de cliente
        String jwt = jwtTokenProvider.generateAccessToken(
            clientId, 
            clientId + "@hcen.internal", 
            Arrays.asList("ROLE_CLIENT", "ROLE_PERIFERICO")
        );
        
        // Calcular fecha de expiración
        long expirationMs = jwtConfig.getJwtAccessTokenExpiration();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
        
        // Guardar nueva sesión en BD
        JWTSession session = new JWTSession(clientId, jwt, expiresAt);
        jwtSessionDAO.save(session);
        
        LOGGER.info("JWT generado y sesión guardada para cliente: " + clientId);
        
        return jwt;
    }
    
    /**
     * Valida las credenciales del cliente contra valores hardcodeados
     */
    private boolean isValidClient(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) {
            return false;
        }
        
        // Validar contra credenciales hardcodeadas en componente-central
        return VALID_CLIENT_ID.equals(clientId) && VALID_CLIENT_SECRET.equals(clientSecret);
    }
    
    /**
     * Valida un JWT en la base de datos
     * @param token JWT a validar
     * @return true si el token es válido y activo
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Buscar sesión en BD
        var sessionOpt = jwtSessionDAO.findByToken(token);
        
        if (sessionOpt.isEmpty()) {
            LOGGER.warning("Token no encontrado en BD");
            return false;
        }
        
        JWTSession session = sessionOpt.get();
        
        // Verificar si está expirado
        if (session.isExpired()) {
            LOGGER.warning("Token expirado para cliente: " + session.getClientId());
            return false;
        }
        
        // Actualizar último uso
        jwtSessionDAO.updateLastUsed(session.getId());
        
        return true;
    }
    
    /**
     * Obtiene el tiempo de expiración del token en segundos
     */
    public long getTokenExpirationSeconds() {
        return jwtConfig.getJwtAccessTokenExpiration() / 1000;
    }
}
