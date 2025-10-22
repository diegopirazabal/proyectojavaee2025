package hcen.central.inus.security.exceptions;

/**
 * Excepción base para errores de autenticación OIDC
 * Todas las excepciones específicas de OIDC heredan de esta
 * 
 * Casos de uso:
 * - Error en flujo de autorización con gub.uy
 * - Fallo en intercambio de código por tokens
 * - Error en validación de ID token
 * - Problemas de comunicación con gub.uy
 */
public class OIDCAuthenticationException extends RuntimeException {
    
    public OIDCAuthenticationException(String message) {
        super(message);
    }
    
    public OIDCAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
