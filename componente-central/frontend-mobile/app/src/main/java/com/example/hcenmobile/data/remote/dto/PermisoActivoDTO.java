package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para representar un permiso de acceso otorgado y activo
 */
public class PermisoActivoDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("documentoId")
    private String documentoId;

    @SerializedName("tipoPermiso")
    private String tipoPermiso; // PROFESIONAL_ESPECIFICO, POR_ESPECIALIDAD, POR_CLINICA

    @SerializedName("ciProfesional")
    private Integer ciProfesional; // Solo si es PROFESIONAL_ESPECIFICO

    @SerializedName("especialidad")
    private String especialidad; // Solo si es POR_ESPECIALIDAD

    @SerializedName("tenantId")
    private String tenantId;

    @SerializedName("nombreClinica")
    private String nombreClinica; // Nombre de la clínica (opcional, para UI)

    @SerializedName("fechaOtorgamiento")
    private String fechaOtorgamiento; // ISO 8601

    @SerializedName("fechaExpiracion")
    private String fechaExpiracion; // ISO 8601

    @SerializedName("estado")
    private String estado; // ACTIVO, REVOCADO, EXPIRADO

    // Constructor vacío
    public PermisoActivoDTO() {
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNombreClinica() {
        return nombreClinica;
    }

    public void setNombreClinica(String nombreClinica) {
        this.nombreClinica = nombreClinica;
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

    /**
     * Helper method para obtener descripción legible del permiso
     */
    public String getDescripcionPermiso() {
        switch (tipoPermiso) {
            case "PROFESIONAL_ESPECIFICO":
                return "Profesional específico (CI: " + ciProfesional + ")";
            case "POR_ESPECIALIDAD":
                return "Todos los " + (especialidad != null ? especialidad : "profesionales de esta especialidad");
            case "POR_CLINICA":
                return "Toda la clínica" + (nombreClinica != null ? " " + nombreClinica : "");
            default:
                return "Permiso " + tipoPermiso;
        }
    }
}
