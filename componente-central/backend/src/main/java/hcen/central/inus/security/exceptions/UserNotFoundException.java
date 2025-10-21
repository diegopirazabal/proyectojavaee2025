package hcen.central.inus.security.exceptions;

/**
 * Excepción lanzada cuando un usuario no se encuentra en el sistema
 * 
 * Casos de uso:
 * - Usuario autenticado en gub.uy pero no existe en base de datos local
 * - Subject (sub) del token no corresponde a ningún UsuarioSalud
 * - Usuario fue desactivado o eliminado del sistema
 * - Email/username no encontrado en OIDCUserDAO
 * 
 * Se usa en:
 * - OIDCUserDAO.findBySub()
 * - OIDCUserDAO.findByEmail()
 * - OIDCAuthenticationService
 * - JWTTokenProvider al validar tokens
 */
public class UserNotFoundException extends OIDCAuthenticationException {
    
    private String userIdentifier; // sub, email, o username
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, String userIdentifier) {
        super(message);
        this.userIdentifier = userIdentifier;
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getUserIdentifier() {
        return userIdentifier;
    }
}
