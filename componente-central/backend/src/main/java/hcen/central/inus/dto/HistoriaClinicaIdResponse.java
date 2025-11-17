package hcen.central.inus.dto;

import hcen.central.inus.entity.historia_clinica;

import java.io.Serializable;

/**
 * DTO para retornar el ID de la historia clínica de un usuario salud
 * basado en su cédula de identidad.
 */
public class HistoriaClinicaIdResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String historiaId; // UUID como String
    private String cedula;

    public HistoriaClinicaIdResponse() {
    }

    public HistoriaClinicaIdResponse(String historiaId, String cedula) {
        this.historiaId = historiaId;
        this.cedula = cedula;
    }

    /**
     * Constructor desde entidad historia_clinica
     */
    public static HistoriaClinicaIdResponse fromEntity(historia_clinica entity) {
        if (entity == null) {
            return null;
        }
        return new HistoriaClinicaIdResponse(
            entity.getId().toString(),
            entity.getUsuario().getCedula()
        );
    }

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
