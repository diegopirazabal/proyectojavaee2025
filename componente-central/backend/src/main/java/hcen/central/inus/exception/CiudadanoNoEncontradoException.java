package hcen.central.inus.exception;

/**
 * Excepción lanzada cuando DNIC no encuentra un ciudadano con el tipo y número de documento proporcionados.
 *
 * Esta excepción se utiliza para encapsular el error de búsqueda en DNIC
 * y proporcionar información detallada sobre el documento consultado.
 */
public class CiudadanoNoEncontradoException extends Exception {

    private static final long serialVersionUID = 1L;

    private String tipoDocumento;
    private String numeroDocumento;

    /**
     * Constructor con tipo y número de documento.
     *
     * @param tipoDocumento Tipo de documento (PA, DO, OTRO)
     * @param numeroDocumento Número de documento (8 dígitos)
     */
    public CiudadanoNoEncontradoException(String tipoDocumento, String numeroDocumento) {
        super("Ciudadano no encontrado en DNIC: " + tipoDocumento + " " + numeroDocumento);
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
    }

    /**
     * Constructor con tipo, número de documento y causa raíz.
     *
     * @param tipoDocumento Tipo de documento (PA, DO, OTRO)
     * @param numeroDocumento Número de documento (8 dígitos)
     * @param cause Excepción original del servicio SOAP
     */
    public CiudadanoNoEncontradoException(String tipoDocumento, String numeroDocumento, Throwable cause) {
        super("Ciudadano no encontrado en DNIC: " + tipoDocumento + " " + numeroDocumento, cause);
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }
}
