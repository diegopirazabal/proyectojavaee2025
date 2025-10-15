package com.hcen.mockdnic.excepciones;

import jakarta.xml.ws.WebFault;

@WebFault(name = "ciudadano_no_encontrado")
public class ciudadano_no_encontrado_exception extends Exception {

    private final String tipoDocumento;
    private final String numeroDocumento;

    public ciudadano_no_encontrado_exception(String tipoDocumento, String numeroDocumento) {
        super(String.format("No se encontró ciudadano con documento %s %s", tipoDocumento, numeroDocumento));
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
