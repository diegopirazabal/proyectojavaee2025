package hcen.central.inus.security.oidc;

import hcen.central.inus.security.config.OIDCConfiguration;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validador de tokens ID Token y Access Token
 * Verifica firma con JWKS, claims (iss, aud, exp, nonce)
 */
@Stateless
public class OIDCTokenValidator {

    private static final Logger LOGGER = Logger.getLogger(OIDCTokenValidator.class.getName());

    @Inject
    private OIDCConfiguration oidcConfig;

    /**
     * Valida un ID Token de gub.uy
     *
     * @param idToken       El ID Token JWT recibido del proveedor OIDC
     * @param expectedNonce El nonce esperado para validar
     * @return Claims del token si es válido
     * @throws JwtException si la validación falla
     */
    public Claims validateIdToken(String idToken, String expectedNonce) throws JwtException {
        try {
            LOGGER.info("Iniciando validación de ID Token");

            // Parsear el header sin validar para obtener el kid (Key ID)
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("ID Token no tiene formato JWT válido");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            LOGGER.fine("Header JWT: " + headerJson);

            // Extraer kid del header (simplificado - en producción usar JSON parser)
            String kid = extractKidFromHeader(headerJson);
            LOGGER.info("Kid extraído del token: " + kid);

            // Obtener la clave pública correspondiente del JWKS
            PublicKey publicKey = getPublicKeyFromJWKS(kid);

            // Validar el token con la clave pública
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build();

            Jws<Claims> jws = parser.parseClaimsJws(idToken);
            Claims claims = jws.getBody();

            // Validar claims estándar
            validateClaims(claims, expectedNonce);

            LOGGER.info("ID Token validado exitosamente para subject: " + claims.getSubject());
            return claims;

        } catch (JwtException e) {
            LOGGER.log(Level.SEVERE, "Error validando ID Token: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado validando ID Token", e);
            throw new JwtException("Error validando ID Token: " + e.getMessage(), e);
        }
    }

    /**
     * Valida los claims del ID Token
     */
    private void validateClaims(Claims claims, String expectedNonce) throws JwtException {
        // Validar issuer
        String issuer = claims.getIssuer();
        if (!oidcConfig.getIssuer().equals(issuer)) {
            throw new JwtException("Issuer inválido. Esperado: " + oidcConfig.getIssuer() + ", Recibido: " + issuer);
        }

        // Validar audience
        String audience = claims.getAudience();
        if (!oidcConfig.getClientId().equals(audience)) {
            throw new JwtException("Audience inválido. Esperado: " + oidcConfig.getClientId() + ", Recibido: " + audience);
        }

        // Validar expiración
        Date expiration = claims.getExpiration();
        if (expiration.before(new Date())) {
            throw new JwtException("ID Token expirado");
        }

        // Validar nonce si se proporcionó
        if (expectedNonce != null) {
            String nonce = claims.get("nonce", String.class);
            if (!expectedNonce.equals(nonce)) {
                throw new JwtException("Nonce inválido");
            }
        }

        LOGGER.info("Claims validados correctamente");
    }

    /**
     * Extrae el kid del header JWT
     */
    private String extractKidFromHeader(String headerJson) {
        // Implementación simplificada
        // En producción, usar Jackson o Jakarta JSON Binding
        int kidIndex = headerJson.indexOf("\"kid\"");
        if (kidIndex == -1) {
            throw new JwtException("Header JWT no contiene kid");
        }
        int colonIndex = headerJson.indexOf(":", kidIndex);
        int quoteStart = headerJson.indexOf("\"", colonIndex) + 1;
        int quoteEnd = headerJson.indexOf("\"", quoteStart);
        return headerJson.substring(quoteStart, quoteEnd);
    }

    /**
     * Obtiene la clave pública del JWKS endpoint usando el kid
     *
     * @param kid Key ID del token
     * @return PublicKey para validar la firma
     */
    private PublicKey getPublicKeyFromJWKS(String kid) throws Exception {
        // TODO: En producción, implementar cache de claves públicas
        // TODO: Hacer llamada HTTP al jwks_uri del proveedor OIDC
        // Por ahora, retornamos una implementación stub que debe ser completada

        LOGGER.warning("ADVERTENCIA: getPublicKeyFromJWKS es un stub. Implementar llamada HTTP a JWKS endpoint.");

        // Ejemplo de cómo construir una clave RSA desde JWKS:
        // 1. Llamar a oidcConfig.getJwksUri() con HttpClient
        // 2. Parsear JSON response para encontrar la key con el kid
        // 3. Extraer los valores 'n' (modulus) y 'e' (exponent)
        // 4. Construir PublicKey como se muestra abajo:

        /*
        String modulusBase64 = ... // extraído del JWKS
        String exponentBase64 = ... // extraído del JWKS

        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);

        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
        */

        throw new UnsupportedOperationException("JWKS retrieval no implementado. Ver TODO en código.");
    }

    /**
     * Valida un Access Token (validación básica)
     * Para gub.uy, el access token puede ser opaco o JWT
     */
    public boolean validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }

        // Si es un JWT, podemos validarlo
        if (accessToken.split("\\.").length == 3) {
            try {
                validateIdToken(accessToken, null);
                return true;
            } catch (JwtException e) {
                LOGGER.log(Level.WARNING, "Access token JWT inválido", e);
                return false;
            }
        }

        // Si es opaco, solo verificamos que no esté vacío
        // La validación real se hace en el proveedor OIDC
        return true;
    }
}
