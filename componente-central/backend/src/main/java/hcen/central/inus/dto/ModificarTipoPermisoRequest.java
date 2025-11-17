package hcen.central.inus.dto;

import java.io.Serializable;

/**
 * DTO para modificar el tipo de permiso de una política de acceso existente.
 * Permite cambiar entre PROFESIONAL_ESPECIFICO, POR_ESPECIALIDAD y POR_CLINICA.
 */
public class ModificarTipoPermisoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipoPermiso; // "PROFESIONAL_ESPECIFICO", "POR_ESPECIALIDAD", "POR_CLINICA"
    private Integer ciProfesional; // Requerido solo si tipoPermiso = PROFESIONAL_ESPECIFICO
    private String especialidad; // Requerido solo si tipoPermiso = POR_ESPECIALIDAD

    public String getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(String tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public Integer getCiProfesional() {
        return ciProfesional;
    }

    public void setCiProfesional(Integer ciProfesional) {
        this.ciProfesional = ciProfesional;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }
}
