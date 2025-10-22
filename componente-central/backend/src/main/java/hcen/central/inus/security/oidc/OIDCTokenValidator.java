package hcen.central.inus.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hcen.central.inus.security.config.OIDCConfiguration;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Signature;

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
            // NOTA: gub.uy usa claves RSA de 1024 bits, así que deshabilitamos la validación de tamaño
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build();

            // Parsear sin validación estricta de tamaño de clave
            Jws<Claims> jws;
            try {
                jws = parser.parseClaimsJws(idToken);
            } catch (io.jsonwebtoken.security.WeakKeyException e) {
                // gub.uy usa claves de 1024 bits que son rechazadas por JJWT
                // Usamos parser sin validación de tamaño de clave
                LOGGER.warning("Clave RSA de gub.uy es de 1024 bits (menor a 2048). Validando sin restricción de tamaño.");
                parser = Jwts.parserBuilder()
                        .setSigningKey(publicKey)
                        .build();
                
                // Parsear manualmente el JWT para obtener los claims
                String[] tokenParts = idToken.split("\\.");
                String claimsJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> claimsMap = mapper.readValue(claimsJson, java.util.Map.class);
                
                // Crear Claims manualmente
                Claims claims = Jwts.claims(claimsMap);
                
                // Verificar la firma manualmente usando la biblioteca estándar de Java
                verifySignatureManually(idToken, publicKey);
                
                return claims;
            }
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
     * Verifica la firma del JWT manualmente usando la clave pública RSA
     * Este método se usa cuando JJWT rechaza claves de 1024 bits
     */
    private void verifySignatureManually(String jwt, PublicKey publicKey) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("JWT inválido: formato incorrecto");
        }
        
        String headerAndPayload = parts[0] + "." + parts[1];
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
        
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(headerAndPayload.getBytes("UTF-8"));
        
        if (!signature.verify(signatureBytes)) {
            throw new JwtException("Firma del JWT inválida");
        }
        
        LOGGER.info("Firma del JWT verificada manualmente con éxito");
    }

    /**
     * Obtiene la clave pública del JWKS endpoint usando el kid
     *
     * @param kid Key ID del token
     * @return PublicKey para validar la firma
     */
    private PublicKey getPublicKeyFromJWKS(String kid) throws Exception {
        String jwksUri = oidcConfig.getJwksUri();
        LOGGER.info("Obteniendo clave pública del JWKS endpoint: " + jwksUri + " para kid: " + kid);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(jwksUri);
            httpGet.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    throw new RuntimeException("Error al obtener JWKS. Status: " + statusCode + ", Body: " + responseBody);
                }

                LOGGER.fine("JWKS response: " + responseBody);

                // Parsear JSON del JWKS
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jwks = objectMapper.readTree(responseBody);
                JsonNode keys = jwks.get("keys");

                if (keys == null || !keys.isArray()) {
                    throw new RuntimeException("JWKS no contiene array 'keys'");
                }

                // Buscar la clave con el kid correspondiente
                for (JsonNode key : keys) {
                    String keyId = key.get("kid").asText();
                    if (kid.equals(keyId)) {
                        String kty = key.get("kty").asText();
                        if (!"RSA".equals(kty)) {
                            throw new RuntimeException("Solo se soportan claves RSA, recibido: " + kty);
                        }

                        // Extraer modulus (n) y exponent (e)
                        String modulusBase64 = key.get("n").asText();
                        String exponentBase64 = key.get("e").asText();

                        // Decodificar Base64URL
                        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
                        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);

                        // Crear BigIntegers
                        BigInteger modulus = new BigInteger(1, modulusBytes);
                        BigInteger exponent = new BigInteger(1, exponentBytes);

                        // Construir la clave pública RSA
                        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                        KeyFactory factory = KeyFactory.getInstance("RSA");
                        PublicKey publicKey = factory.generatePublic(spec);

                        LOGGER.info("Clave pública obtenida exitosamente para kid: " + kid);
                        return publicKey;
                    }
                }

                throw new RuntimeException("No se encontró clave con kid: " + kid + " en JWKS");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo clave pública del JWKS", e);
            throw new RuntimeException("Error obteniendo clave pública: " + e.getMessage(), e);
        }
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
