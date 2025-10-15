package com.hcen.mockdnic.ws.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "ciudadano_no_encontrado_fault",
        namespace = "http://hcen.com/soap/ciudadano",
        propOrder = {"tipoDocumento", "numeroDocumento"}
)
public class ciudadano_no_encontrado_fault {

    @XmlElement(name = "tipoDoc", namespace = "http://hcen.com/soap/ciudadano", required = true)
    private String tipoDocumento;

    @XmlElement(name = "nroDoc", namespace = "http://hcen.com/soap/ciudadano", required = true)
    private String numeroDocumento;

    public ciudadano_no_encontrado_fault() {
    }

    public ciudadano_no_encontrado_fault(String tipoDocumento, String numeroDocumento) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }
}
