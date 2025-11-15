package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO que representa un registro de documento dentro de la historia clínica central.
 */
public class HistoriaClinicaDocumentoDTO {

    @SerializedName("historiaId")
    private String historiaId;

    @SerializedName("documentoId")
    private String documentoId;

    @SerializedName("tenantId")
    private String tenantId;

    @SerializedName("usuarioCedula")
    private String usuarioCedula;

    @SerializedName("fechaRegistro")
    private String fechaRegistro;

    @SerializedName("fechaDocumento")
    private String fechaDocumento;

    @SerializedName("motivoConsulta")
    private String motivoConsulta;

    @SerializedName("profesional")
    private String profesional;

    public String getHistoriaId() {
        return historiaId;
    }

    public void setHistoriaId(String historiaId) {
        this.historiaId = historiaId;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsuarioCedula() {
        return usuarioCedula;
    }

    public void setUsuarioCedula(String usuarioCedula) {
        this.usuarioCedula = usuarioCedula;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getFechaDocumento() {
        return fechaDocumento;
    }

    public void setFechaDocumento(String fechaDocumento) {
        this.fechaDocumento = fechaDocumento;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String motivoConsulta) {
        this.motivoConsulta = motivoConsulta;
    }

    public String getProfesional() {
        return profesional;
    }

    public void setProfesional(String profesional) {
        this.profesional = profesional;
    }
}
