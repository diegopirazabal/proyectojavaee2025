package com.hcen.mockdnic.excepciones;

import jakarta.xml.ws.WebFault;

@WebFault(name = "CiudadanoNoEncontrado")
public class CiudadanoNoEncontradoException extends Exception {

    private final String tipoDocumento;
    private final String numeroDocumento;

    public CiudadanoNoEncontradoException(String tipoDocumento, String numeroDocumento) {
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
