package com.hcen.periferico.entity;

import com.hcen.periferico.enums.TipoSincronizacion;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para tracking de sincronizaciones fallidas con el componente central.
 * Funciona como Dead Letter Queue (DLQ) local para usuarios y documentos clínicos
 * que no pudieron ser sincronizados con el componente central.
 *
 * Permite:
 * - Reintentos manuales
 * - Batch jobs de resincronización
 * - Auditoría de errores de comunicación
 * - Distinguir entre sincronizaciones de usuarios y documentos mediante el campo 'tipo'
 */
@Entity
@Table(name = "sincronizacion_pendiente",
       indexes = {
           @Index(name = "idx_usuario_ref", columnList = "usuario_cedula,tenant_id"),
           @Index(name = "idx_created", columnList = "created_at"),
           @Index(name = "idx_tipo_estado", columnList = "tipo,estado")
       })
public class SincronizacionPendiente implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============ IDENTIFICACIÓN ============

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    // ============ DISCRIMINADOR Y REFERENCIAS ============

    /**
     * Tipo de sincronización (USUARIO o DOCUMENTO)
     * Campo discriminador que determina qué tipo de entidad se está sincronizando
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoSincronizacion tipo = TipoSincronizacion.USUARIO;

    /**
     * Cédula del usuario asociado a esta sincronización
     * - Para tipo USUARIO: usuario a sincronizar
     * - Para tipo DOCUMENTO: paciente dueño del documento
     */
    @Column(name = "usuario_cedula", nullable = false, length = 20)
    private String usuarioCedula;

    /**
     * ID del documento clínico a sincronizar (solo para tipo DOCUMENTO)
     * NULL para sincronizaciones de tipo USUARIO
     */
    @Column(name = "documento_id", columnDefinition = "UUID")
    private UUID documentoId;

    /**
     * ID del tenant (clínica) al que pertenece esta sincronización
     * Necesario para contexto multi-tenant
     */
    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    // ============ ESTADO Y TRACKING ============

    /**
     * Estado actual de la sincronización (PENDIENTE, ERROR, RESUELTA, CANCELADA)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoSincronizacion estado = EstadoSincronizacion.PENDIENTE;

    /**
     * Contador de intentos de sincronización realizados
     */
    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    /**
     * Mensaje del último error ocurrido (solo si estado = ERROR)
     */
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    // ============ AUDITORÍA ============

    /**
     * Fecha y hora de creación del registro
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de última actualización
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (intentos == null) {
            intentos = 0;
        }
        if (estado == null) {
            estado = EstadoSincronizacion.PENDIENTE;
        }
        if (tipo == null) {
            tipo = TipoSincronizacion.USUARIO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructores
    public SincronizacionPendiente() {
    }

    public SincronizacionPendiente(String usuarioCedula, UUID tenantId) {
        this.usuarioCedula = usuarioCedula;
        this.tenantId = tenantId;
        this.tipo = TipoSincronizacion.USUARIO;
    }

    public SincronizacionPendiente(String usuarioCedula, UUID tenantId, TipoSincronizacion tipo) {
        this.usuarioCedula = usuarioCedula;
        this.tenantId = tenantId;
        this.tipo = tipo;
    }

    public SincronizacionPendiente(String usuarioCedula, UUID tenantId, UUID documentoId) {
        this.usuarioCedula = usuarioCedula;
        this.tenantId = tenantId;
        this.tipo = TipoSincronizacion.DOCUMENTO;
        this.documentoId = documentoId;
    }

    // Métodos de utilidad
    public void incrementarIntentos() {
        this.intentos++;
    }

    public void registrarError(String error) {
        this.ultimoError = error;
        this.intentos++;
        this.estado = EstadoSincronizacion.ERROR;
    }

    public void marcarComoResuelta() {
        this.estado = EstadoSincronizacion.RESUELTA;
    }

    public void marcarComoCancelada() {
        this.estado = EstadoSincronizacion.CANCELADA;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsuarioCedula() {
        return usuarioCedula;
    }

    public void setUsuarioCedula(String usuarioCedula) {
        this.usuarioCedula = usuarioCedula;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(String ultimoError) {
        this.ultimoError = ultimoError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public EstadoSincronizacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSincronizacion estado) {
        this.estado = estado;
    }

    public TipoSincronizacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoSincronizacion tipo) {
        this.tipo = tipo;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    @Override
    public String toString() {
        return "SincronizacionPendiente{" +
                "id=" + id +
                ", tipo=" + tipo +
                ", usuarioCedula='" + usuarioCedula + '\'' +
                ", documentoId=" + documentoId +
                ", tenantId=" + tenantId +
                ", estado=" + estado +
                ", intentos=" + intentos +
                ", ultimoError='" + (ultimoError != null ? ultimoError.substring(0, Math.min(50, ultimoError.length())) + "..." : null) + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * Estados posibles de una sincronización pendiente
     */
    public enum EstadoSincronizacion {
        PENDIENTE,   // Esperando reintento
        ERROR,       // Falló en último intento
        RESUELTA,    // Sincronización exitosa
        CANCELADA    // Cancelada manualmente
    }
}
