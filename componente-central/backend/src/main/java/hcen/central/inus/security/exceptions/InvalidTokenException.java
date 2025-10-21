package hcen.central.inus.security.exceptions;

/**
 * Excepción lanzada cuando un token es inválido
 * 
 * Casos de uso:
 * - Token expirado (ID token de gub.uy o JWT propio)
 * - Firma inválida
 * - Claims requeridos faltantes (sub, iss, aud, etc.)
 * - Token malformado (no es un JWT válido)
 * - Issuer no coincide
 * - Audience no coincide
 * - Nonce inválido (OIDC)
 * 
 * Se usa en:
 * - OIDCTokenValidator (para tokens de gub.uy)
 * - JWTTokenProvider (para tokens propios)
 */
public class InvalidTokenException extends OIDCAuthenticationException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
