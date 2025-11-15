package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para representar una solicitud de acceso a documento clínico pendiente de aprobación
 */
public class SolicitudAccesoDTO {

    @SerializedName("id")
    private String id; // ID de la notificación

    @SerializedName("tipo")
    private String tipo; // SOLICITUD_ACCESO

    @SerializedName("mensaje")
    private String mensaje; // Mensaje descriptivo

    @SerializedName("estado")
    private String estado; // PENDIENTE, APROBADA, RECHAZADA

    @SerializedName("fechaCreacion")
    private String fechaCreacion; // ISO 8601

    // Datos específicos de la solicitud
    @SerializedName("documentoId")
    private String documentoId;

    @SerializedName("profesionalCi")
    private int profesionalCi;

    @SerializedName("profesionalNombre")
    private String profesionalNombre;

    @SerializedName("especialidad")
    private String especialidad;

    @SerializedName("tenantId")
    private String tenantId;

    @SerializedName("nombreClinica")
    private String nombreClinica;

    @SerializedName("fechaDocumento")
    private String fechaDocumento;

    @SerializedName("motivoConsulta")
    private String motivoConsulta;

    @SerializedName("diagnostico")
    private String diagnostico;

    // Constructor vacío
    public SolicitudAccesoDTO() {
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

    public int getProfesionalCi() {
        return profesionalCi;
    }

    public void setProfesionalCi(int profesionalCi) {
        this.profesionalCi = profesionalCi;
    }

    public String getProfesionalNombre() {
        return profesionalNombre;
    }

    public void setProfesionalNombre(String profesionalNombre) {
        this.profesionalNombre = profesionalNombre;
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

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }
}
