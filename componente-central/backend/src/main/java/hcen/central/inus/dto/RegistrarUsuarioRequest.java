package hcen.central.inus.dto;

import hcen.central.inus.enums.TipoDocumento;
import java.io.Serializable;

/**
 * DTO para solicitud de registro de usuario de salud.
 * El componente periférico solo envía el documento del usuario.
 * Los demás datos (nombre, email, etc.) se manejan localmente en el periférico.
 */
public class RegistrarUsuarioRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private TipoDocumento tipoDocumento;

    // Constructores
    public RegistrarUsuarioRequest() {}

    public RegistrarUsuarioRequest(String cedula, TipoDocumento tipoDocumento) {
        this.cedula = cedula;
        this.tipoDocumento = tipoDocumento;
    }

    // Getters y Setters
    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    @Override
    public String toString() {
        return "RegistrarUsuarioRequest{" +
                "cedula='" + cedula + '\'' +
                ", tipoDocumento=" + tipoDocumento +
                '}';
    }
}
