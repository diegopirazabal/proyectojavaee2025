package hcen.frontend.admin.dto;

import java.io.Serializable;

public class historia_clinica_id_response implements Serializable {

    private static final long serialVersionUID = 1L;

    private String historiaId;
    private String cedula;

    public String getHistoriaId() {
        return historiaId;
    }

    public void setHistoriaId(String historiaId) {
        this.historiaId = historiaId;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }
}
