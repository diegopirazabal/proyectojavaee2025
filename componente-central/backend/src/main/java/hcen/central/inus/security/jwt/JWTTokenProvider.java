package hcen.central.inus.security.jwt;

import hcen.central.inus.security.exceptions.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provider para generación y validación de tokens JWT internos
 * Usa la biblioteca jjwt (io.jsonwebtoken)
 * 
 * IMPORTANTE: Estos son tokens PROPIOS del sistema hcen.uy
 * NO confundir con los ID tokens de gub.uy (esos se validan en OIDCTokenValidator)
 * 
 * Flujo:
 * 1. Usuario se autentica con gub.uy (OIDC)
 * 2. Sistema valida ID Token de gub.uy
 * 3. Sistema genera JWT PROPIO con claims del usuario
 * 4. Frontend/Móvil usa JWT propio en requests subsecuentes
 */
@Stateless
public class JWTTokenProvider {
    
    private static final Logger logger = Logger.getLogger(JWTTokenProvider.class.getName());
    
    @EJB
    private JWTConfiguration jwtConfig;
    
    /**
     * Genera un access token JWT para el usuario (versión simplificada)
     * @param userSub Subject del usuario (del OIDC)
     * @param roles Lista de roles del usuario
     * @return JWT firmado
     */
    public String generateAccessToken(String userSub, List<String> roles) {
        return generateAccessToken(userSub, null, roles);
    }
    
    /**
     * Genera un access token JWT para el usuario
     * @param userSub Subject del usuario (del OIDC)
     * @param email Email del usuario
     * @param roles Lista de roles del usuario
     * @return JWT firmado
     */
    public String generateAccessToken(String userSub, String email, List<String> roles) {
        return generateAccessToken(userSub, email, null, null, roles);
    }
    
    /**
     * Genera un access token JWT para el usuario incluyendo documento
     * @param userSub Subject del usuario (del OIDC)
     * @param email Email del usuario
     * @param docType Tipo de documento del usuario
     * @param docNumber Número de documento del usuario
     * @param roles Lista de roles del usuario
     * @return JWT firmado
     */
    public String generateAccessToken(String userSub, String email, String docType, String docNumber, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getJwtAccessTokenExpiration());
        
        return Jwts.builder()
                .setSubject(userSub)
                .setIssuer(jwtConfig.getJwtIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("email", email)
                .claim("docType", docType)
                .claim("docNumber", docNumber)
                .claim("roles", roles)
                .claim("type", "access")
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Genera un refresh token para renovar access tokens
     * @param userSub Subject del usuario
     * @return JWT refresh token
     */
    public String generateRefreshToken(String userSub) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getJwtRefreshTokenExpiration());
        
        return Jwts.builder()
                .setSubject(userSub)
                .setIssuer(jwtConfig.getJwtIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("type", "refresh")
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Valida un access token JWT
     * @param token Access token a validar
     * @return Claims del token
     * @throws InvalidTokenException si el token es inválido
     */
    public Claims validateAccessToken(String token) {
        Claims claims = validateToken(token);
        String tokenType = (String) claims.get("type");
        if (!"access".equals(tokenType)) {
            throw new InvalidTokenException("Token no es de tipo access");
        }
        return claims;
    }
    
    /**
     * Valida un refresh token JWT
     * @param token Refresh token a validar
     * @return Claims del token
     * @throws InvalidTokenException si el token es inválido
     */
    public Claims validateRefreshToken(String token) {
        Claims claims = validateToken(token);
        String tokenType = (String) claims.get("type");
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Token no es de tipo refresh");
        }
        return claims;
    }
    
    /**
     * Valida un token JWT y retorna sus claims
     * @param token JWT a validar
     * @return Claims del token
     * @throws InvalidTokenException si el token es inválido
     */
    private Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                    
        } catch (ExpiredJwtException e) {
            // No verificar expiración - retornar los claims del token expirado
            logger.info("Token expirado pero se permite su uso: " + e.getMessage());
            return e.getClaims();
        } catch (UnsupportedJwtException e) {
            logger.warning("Token no soportado: " + e.getMessage());
            throw new InvalidTokenException("Token no soportado", e);
        } catch (MalformedJwtException e) {
            logger.warning("Token malformado: " + e.getMessage());
            throw new InvalidTokenException("Token malformado", e);
        } catch (SignatureException e) {
            logger.warning("Firma inválida: " + e.getMessage());
            throw new InvalidTokenException("Firma JWT inválida", e);
        } catch (IllegalArgumentException e) {
            logger.warning("Token vacío: " + e.getMessage());
            throw new InvalidTokenException("Token vacío o nulo", e);
        }
    }
    
    /**
     * Verifica si un token está expirado
     * @param token JWT
     * @return true si expirado, false si válido
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (InvalidTokenException e) {
            return true;
        }
    }
    
    /**
     * Extrae el subject (userSub) del token
     * @param token JWT
     * @return subject del usuario
     */
    public String getUserSubFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }
    
    /**
     * Extrae los roles del token
     * @param token JWT
     * @return Lista de roles
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        return (List<String>) claims.get("roles");
    }
    
    /**
     * Extrae el email del token
     * @param token JWT
     * @return email del usuario
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return (String) claims.get("email");
    }
    
    /**
     * Extrae el tipo de documento del token
     * @param token JWT
     * @return tipo de documento del usuario
     */
    public String getDocTypeFromToken(String token) {
        Claims claims = validateToken(token);
        return (String) claims.get("docType");
    }
    
    /**
     * Extrae el número de documento del token
     * @param token JWT
     * @return número de documento del usuario
     */
    public String getDocNumberFromToken(String token) {
        Claims claims = validateToken(token);
        return (String) claims.get("docNumber");
    }
    
    /**
     * Renueva un access token usando un refresh token
     * @param refreshToken Refresh token válido
     * @param email Email del usuario
     * @param roles Roles actualizados
     * @return Nuevo access token
     */
    public String refreshAccessToken(String refreshToken, String email, List<String> roles) {
        // Validar que sea un refresh token
        Claims claims = validateToken(refreshToken);
        String tokenType = (String) claims.get("type");
        
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Token no es de tipo refresh");
        }
        
        String userSub = claims.getSubject();
        return generateAccessToken(userSub, email, roles);
    }
    
    /**
     * Obtiene la clave de firma desde la configuración
     * @return SecretKey para firmar/validar JWT
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
