package com.example.hcenmobile.data.model;

/**
 * Modelo utilizado por la UI para mostrar un documento clínico resumido.
 */
public class HistoriaClinicaItem {

    private final String documentoId;
    private final String tenantId;
    private final String usuarioCedula;
    private final String motivoConsulta;
    private final String fechaResumen;
    private final String fechaDocumento;
    private final String fechaRegistro;
    private final String profesional;
    private final String nombreClinica;

    public HistoriaClinicaItem(String documentoId,
                               String tenantId,
                               String usuarioCedula,
                               String motivoConsulta,
                               String fechaResumen,
                               String fechaDocumento,
                               String fechaRegistro,
                               String profesional,
                               String nombreClinica) {
        this.documentoId = documentoId;
        this.tenantId = tenantId;
        this.usuarioCedula = usuarioCedula;
        this.motivoConsulta = motivoConsulta;
        this.fechaResumen = fechaResumen;
        this.fechaDocumento = fechaDocumento;
        this.fechaRegistro = fechaRegistro;
        this.profesional = profesional;
        this.nombreClinica = nombreClinica;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public String getFecha() {
        return fechaResumen;
    }

    public String getFechaDocumento() {
        return fechaDocumento;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getUsuarioCedula() {
        return usuarioCedula;
    }

    public String getProfesional() {
        return profesional;
    }

    public String getNombreClinica() {
        return nombreClinica;
    }
}
