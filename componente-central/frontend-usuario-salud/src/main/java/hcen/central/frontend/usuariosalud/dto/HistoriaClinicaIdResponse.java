package hcen.central.frontend.usuariosalud.dto;

import java.io.Serializable;

/**
 * DTO de response para obtener el ID de historia clínica por cédula.
 */
public class HistoriaClinicaIdResponse implements Serializable {

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
