package com.hcen.mockdnic.ws.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SolicitudCiudadano", namespace = "http://hcen.com/soap/ciudadano")
public class SolicitudCiudadano {

    @XmlElement(name = "tipoDoc", required = true, namespace = "http://hcen.com/soap/ciudadano")
    @NotBlank
    @Size(max = 4)
    @Pattern(regexp = "PA|DO|OTRO")
    private String tipoDocumento;

    @XmlElement(name = "nroDoc", required = true, namespace = "http://hcen.com/soap/ciudadano")
    @NotBlank
    @Pattern(regexp = "[0-9]{8}")
    private String numeroDocumento;

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
