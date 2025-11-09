package hcen.frontend.admin.service;

/**
 * Excepción que indica que la llamada al backend fue rechazada por falta de autenticación.
 */
public class ApiUnauthorizedException extends RuntimeException {

    public ApiUnauthorizedException(String message) {
        super(message);
    }

    public ApiUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

