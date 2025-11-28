package hcen.central.inus.dto;

import hcen.central.inus.entity.politica_acceso;
import hcen.central.inus.enums.EstadoPermiso;
import hcen.central.inus.enums.TipoPermiso;

import jakarta.json.bind.annotation.JsonbDateFormat;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para transferir datos de política de acceso a documentos clínicos
 */
public class PoliticaAccesoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID historiaClinicaId;
    private UUID documentoId;
    private TipoPermiso tipoPermiso;
    private Integer ciProfesional;
    private UUID tenantId;
    private String especialidad;
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaOtorgamiento;

    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaExpiracion;
    private EstadoPermiso estado;
    private String motivoRevocacion;
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaRevocacion;

    // Constructores

    public PoliticaAccesoDTO() {
    }

    /**
     * Constructor para crear DTO desde entidad
     */
    public PoliticaAccesoDTO(politica_acceso entidad) {
        this.id = entidad.getId();
        this.historiaClinicaId = entidad.getHistoriaClinica() != null ? entidad.getHistoriaClinica().getId() : null;
        this.documentoId = entidad.getDocumentoId();
        this.tipoPermiso = entidad.getTipoPermiso();
        this.ciProfesional = entidad.getCiProfesional();
        this.tenantId = entidad.getTenantId();
        this.especialidad = entidad.getEspecialidad();
        this.fechaOtorgamiento = entidad.getFechaOtorgamiento();
        this.fechaExpiracion = entidad.getFechaExpiracion();
        this.estado = entidad.getEstado();
        this.motivoRevocacion = entidad.getMotivoRevocacion();
        this.fechaRevocacion = entidad.getFechaRevocacion();
    }

    /**
     * Constructor para crear nuevo permiso (sin ID, estado ACTIVO por defecto)
     */
    public PoliticaAccesoDTO(UUID historiaClinicaId, UUID documentoId, TipoPermiso tipoPermiso,
                             Integer ciProfesional, UUID tenantId, String especialidad,
                             LocalDateTime fechaExpiracion) {
        this.historiaClinicaId = historiaClinicaId;
        this.documentoId = documentoId;
        this.tipoPermiso = tipoPermiso;
        this.ciProfesional = ciProfesional;
        this.tenantId = tenantId;
        this.especialidad = especialidad;
        this.fechaOtorgamiento = LocalDateTime.now();
        this.fechaExpiracion = fechaExpiracion;
        this.estado = EstadoPermiso.ACTIVO;
    }

    // Getters y Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getHistoriaClinicaId() {
        return historiaClinicaId;
    }

    public void setHistoriaClinicaId(UUID historiaClinicaId) {
        this.historiaClinicaId = historiaClinicaId;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public TipoPermiso getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(TipoPermiso tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public Integer getCiProfesional() {
        return ciProfesional;
    }

    public void setCiProfesional(Integer ciProfesional) {
        this.ciProfesional = ciProfesional;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
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

    public EstadoPermiso getEstado() {
        return estado;
    }

    public void setEstado(EstadoPermiso estado) {
        this.estado = estado;
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

    @Override
    public String toString() {
        return "PoliticaAccesoDTO{" +
                "id=" + id +
                ", historiaClinicaId=" + historiaClinicaId +
                ", documentoId=" + documentoId +
                ", tipoPermiso=" + tipoPermiso +
                ", ciProfesional=" + ciProfesional +
                ", tenantId=" + tenantId +
                ", especialidad='" + especialidad + '\'' +
                ", fechaExpiracion=" + fechaExpiracion +
                ", estado=" + estado +
                '}';
    }
}
