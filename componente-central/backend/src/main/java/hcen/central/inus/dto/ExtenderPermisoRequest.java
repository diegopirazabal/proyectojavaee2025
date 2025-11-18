package hcen.central.inus.dto;

import java.io.Serializable;

/**
 * DTO para solicitar la extensión de la fecha de expiración
 * de una política de acceso existente.
 */
public class ExtenderPermisoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nuevaFechaExpiracion; // ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss)

    public String getNuevaFechaExpiracion() {
        return nuevaFechaExpiracion;
    }

    public void setNuevaFechaExpiracion(String nuevaFechaExpiracion) {
        this.nuevaFechaExpiracion = nuevaFechaExpiracion;
    }
}
