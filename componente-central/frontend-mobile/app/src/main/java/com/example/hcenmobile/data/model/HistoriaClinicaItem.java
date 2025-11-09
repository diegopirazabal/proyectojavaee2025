package com.example.hcenmobile.data.model;

/**
 * Modelo utilizado por la UI para mostrar un documento clínico resumido.
 */
public class HistoriaClinicaItem {

    private final String documentoId;
    private final String tenantId;
    private final String motivoConsulta;
    private final String fecha;
    private final String profesional;

    public HistoriaClinicaItem(String documentoId,
                               String tenantId,
                               String motivoConsulta,
                               String fecha,
                               String profesional) {
        this.documentoId = documentoId;
        this.tenantId = tenantId;
        this.motivoConsulta = motivoConsulta;
        this.fecha = fecha;
        this.profesional = profesional;
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
        return fecha;
    }

    public String getProfesional() {
        return profesional;
    }
}
