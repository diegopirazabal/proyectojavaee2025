package hcen.central.frontend.usuariosalud.dto;

import java.io.Serializable;

/**
 * DTO para representar una política de acceso a un documento clínico.
 * Usado por los usuarios salud para gestionar sus permisos.
 */
public class PoliticaAccesoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String historiaClinicaId;
    private String documentoId;
    private String tipoPermiso; // PROFESIONAL_ESPECIFICO, POR_ESPECIALIDAD, POR_CLINICA
    private Integer ciProfesional;
    private String tenantId;
    private String especialidad;
    private String fechaOtorgamiento;
    private String fechaExpiracion;
    private String estado; // ACTIVO, REVOCADO, EXPIRADO
    private String motivoRevocacion;
    private String fechaRevocacion;

    // Getters y Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHistoriaClinicaId() {
        return historiaClinicaId;
    }

    public void setHistoriaClinicaId(String historiaClinicaId) {
        this.historiaClinicaId = historiaClinicaId;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getFechaOtorgamiento() {
        return fechaOtorgamiento;
    }

    public void setFechaOtorgamiento(String fechaOtorgamiento) {
        this.fechaOtorgamiento = fechaOtorgamiento;
    }

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(String fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public void setMotivoRevocacion(String motivoRevocacion) {
        this.motivoRevocacion = motivoRevocacion;
    }

    public String getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(String fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }

    // Métodos helper para la UI

    /**
     * Retorna una descripción legible del permiso
     */
    public String getDescripcionPermiso() {
        if (tipoPermiso == null) {
            return "Tipo desconocido";
        }

        switch (tipoPermiso) {
            case "PROFESIONAL_ESPECIFICO":
                return "Profesional CI: " + (ciProfesional != null ? ciProfesional : "?");
            case "POR_ESPECIALIDAD":
                return "Especialidad: " + (especialidad != null ? especialidad : "?");
            case "POR_CLINICA":
                return "Toda la clínica";
            default:
                return "Tipo: " + tipoPermiso;
        }
    }

    /**
     * Verifica si el permiso está activo
     */
    public boolean isActivo() {
        return "ACTIVO".equals(estado);
    }

    /**
     * Verifica si el permiso está revocado
     */
    public boolean isRevocado() {
        return "REVOCADO".equals(estado);
    }

    /**
     * Verifica si el permiso está expirado
     */
    public boolean isExpirado() {
        return "EXPIRADO".equals(estado);
    }

    /**
     * Retorna el ID truncado para mostrar en UI
     */
    public String getIdCorto() {
        if (id != null && id.length() > 8) {
            return id.substring(0, 8) + "...";
        }
        return id;
    }

    /**
     * Retorna el ID del documento truncado
     */
    public String getDocumentoIdCorto() {
        if (documentoId != null && documentoId.length() > 8) {
            return documentoId.substring(0, 8) + "...";
        }
        return documentoId;
    }
}
