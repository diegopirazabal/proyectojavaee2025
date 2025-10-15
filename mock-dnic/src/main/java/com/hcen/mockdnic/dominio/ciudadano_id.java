package com.hcen.mockdnic.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ciudadano_id implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "tipo_doc", length = 4, nullable = false)
    private String tipoDocumento;

    @Column(name = "nro_doc", length = 8, nullable = false)
    private String numeroDocumento;

    public ciudadano_id() {
    }

    public ciudadano_id(String tipoDocumento, String numeroDocumento) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ciudadano_id that = (ciudadano_id) o;
        return Objects.equals(tipoDocumento, that.tipoDocumento)
                && Objects.equals(numeroDocumento, that.numeroDocumento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipoDocumento, numeroDocumento);
    }
}
