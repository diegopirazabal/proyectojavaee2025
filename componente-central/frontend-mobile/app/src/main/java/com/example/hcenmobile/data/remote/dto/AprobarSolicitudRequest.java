package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request para aprobar una solicitud de acceso a documento clínico
 */
public class AprobarSolicitudRequest {

    @SerializedName("notificacionId")
    private String notificacionId;

    @SerializedName("cedulaPaciente")
    private String cedulaPaciente;

    @SerializedName("documentoId")
    private String documentoId;

    @SerializedName("tipoPermiso")
    private String tipoPermiso; // PROFESIONAL_ESPECIFICO | POR_ESPECIALIDAD | POR_CLINICA

    @SerializedName("profesionalCi")
    private Integer profesionalCi; // Solo para PROFESIONAL_ESPECIFICO

    @SerializedName("especialidad")
    private String especialidad; // Solo para POR_ESPECIALIDAD

    @SerializedName("tenantId")
    private String tenantId;

    @SerializedName("fechaExpiracion")
    private String fechaExpiracion;

    // Constructor vacío
    public AprobarSolicitudRequest() {
    }

    // Constructor con todos los campos
    public AprobarSolicitudRequest(String notificacionId, String cedulaPaciente,
                                   String documentoId, String tipoPermiso,
                                   Integer profesionalCi, String especialidad,
                                   String tenantId) {
        this.notificacionId = notificacionId;
        this.cedulaPaciente = cedulaPaciente;
        this.documentoId = documentoId;
        this.tipoPermiso = tipoPermiso;
        this.profesionalCi = profesionalCi;
        this.especialidad = especialidad;
        this.tenantId = tenantId;
    }

    // Getters y Setters
    public String getNotificacionId() {
        return notificacionId;
    }

    public void setNotificacionId(String notificacionId) {
        this.notificacionId = notificacionId;
    }

    public String getCedulaPaciente() {
        return cedulaPaciente;
    }

    public void setCedulaPaciente(String cedulaPaciente) {
        this.cedulaPaciente = cedulaPaciente;
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

    public Integer getProfesionalCi() {
        return profesionalCi;
    }

    public void setProfesionalCi(Integer profesionalCi) {
        this.profesionalCi = profesionalCi;
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

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(String fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
