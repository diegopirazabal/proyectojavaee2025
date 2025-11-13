package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para auditoría de solicitudes de acceso a documentos clínicos.
 * Registra cuando un profesional solicita acceso a un documento que no puede ver.
 * Permite controlar que no se envíen solicitudes duplicadas en un período de 24 horas.
 */
@Entity
@Table(name = "SOLICITUD_ACCESO_DOCUMENTO", indexes = {
    @Index(name = "idx_solicitud_documento", columnList = "DOCUMENTO_ID"),
    @Index(name = "idx_solicitud_profesional", columnList = "PROFESIONAL_CI, TENANT_ID"),
    @Index(name = "idx_solicitud_fecha", columnList = "FECHA_SOLICITUD")
})
public class solicitud_acceso_documento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    /**
     * UUID del documento clínico al que se solicita acceso
     */
    @Column(name = "DOCUMENTO_ID", nullable = false, columnDefinition = "UUID")
    private UUID documentoId;

    /**
     * CI del profesional que solicita acceso
     */
    @Column(name = "PROFESIONAL_CI", nullable = false)
    private Integer profesionalCi;

    /**
     * UUID de la clínica (tenant_id)
     */
    @Column(name = "TENANT_ID", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    /**
     * Cédula del paciente dueño del documento
     */
    @Column(name = "CEDULA_PACIENTE", nullable = false, length = 20)
    private String cedulaPaciente;

    /**
     * Fecha y hora en que se envió la solicitud
     */
    @Column(name = "FECHA_SOLICITUD", nullable = false)
    private LocalDateTime fechaSolicitud;

    /**
     * Estado de la solicitud
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 30, nullable = false)
    private EstadoSolicitud estado;

    /**
     * Fecha de respuesta del paciente (null si aún no respondió)
     */
    @Column(name = "FECHA_RESPUESTA")
    private LocalDateTime fechaRespuesta;

    // Constructores

    public solicitud_acceso_documento() {
        this.fechaSolicitud = LocalDateTime.now();
        this.estado = EstadoSolicitud.PENDIENTE;
    }

    // Getters y Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public Integer getProfesionalCi() {
        return profesionalCi;
    }

    public void setProfesionalCi(Integer profesionalCi) {
        this.profesionalCi = profesionalCi;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getCedulaPaciente() {
        return cedulaPaciente;
    }

    public void setCedulaPaciente(String cedulaPaciente) {
        this.cedulaPaciente = cedulaPaciente;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    // Métodos de utilidad

    /**
     * Verifica si han pasado más de 24 horas desde la última solicitud
     */
    public boolean puedeVolverASolicitar() {
        if (this.fechaSolicitud == null) {
            return true;
        }
        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        return this.fechaSolicitud.isBefore(hace24Horas);
    }

    /**
     * Marca la solicitud como aprobada
     */
    public void aprobar() {
        this.estado = EstadoSolicitud.APROBADA;
        this.fechaRespuesta = LocalDateTime.now();
    }

    /**
     * Marca la solicitud como rechazada
     */
    public void rechazar() {
        this.estado = EstadoSolicitud.RECHAZADA;
        this.fechaRespuesta = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaSolicitud == null) {
            this.fechaSolicitud = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoSolicitud.PENDIENTE;
        }
    }

    // Enum para estado de solicitud

    public enum EstadoSolicitud {
        /**
         * Solicitud enviada, esperando respuesta del paciente
         */
        PENDIENTE,

        /**
         * Paciente aprobó el acceso (se creó politica_acceso)
         */
        APROBADA,

        /**
         * Paciente rechazó el acceso
         */
        RECHAZADA
    }
}
