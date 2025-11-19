package hcen.frontend.admin.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class politica_acceso_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String historiaClinicaId;
    private String documentoId;
    private String tipoPermiso;
    private String tenantId;
    private String estado;
    private Integer ciProfesional;
    private String especialidad;
    private LocalDateTime fechaOtorgamiento;
    private LocalDateTime fechaExpiracion;
    private String motivoRevocacion;
    private LocalDateTime fechaRevocacion;

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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public LocalDateTime getFechaOtorgamiento() {
        return fechaOtorgamiento;
    }

    public void setFechaOtorgamiento(LocalDateTime fechaOtorgamiento) {
        this.fechaOtorgamiento = fechaOtorgamiento;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public void setMotivoRevocacion(String motivoRevocacion) {
        this.motivoRevocacion = motivoRevocacion;
    }

    public LocalDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(LocalDateTime fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }
}
