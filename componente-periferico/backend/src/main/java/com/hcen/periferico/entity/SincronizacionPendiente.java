package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para tracking de sincronizaciones fallidas con el componente central.
 * Funciona como Dead Letter Queue (DLQ) local para usuarios que no pudieron
 * ser sincronizados con el componente central.
 *
 * Permite:
 * - Reintentos manuales
 * - Batch jobs de resincronización
 * - Auditoría de errores de comunicación
 */
@Entity
@Table(name = "sincronizacion_pendiente",
       indexes = {
           @Index(name = "idx_usuario_ref", columnList = "usuario_cedula,tenant_id"),
           @Index(name = "idx_created", columnList = "created_at")
       })
public class SincronizacionPendiente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "usuario_cedula", nullable = false, length = 20)
    private String usuarioCedula;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Estado de la sincronización pendiente
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoSincronizacion estado = EstadoSincronizacion.PENDIENTE;

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

    @Override
    public String toString() {
        return "SincronizacionPendiente{" +
                "id=" + id +
                ", usuarioCedula='" + usuarioCedula + '\'' +
                ", tenantId=" + tenantId +
                ", intentos=" + intentos +
                ", estado=" + estado +
                ", createdAt=" + createdAt +
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
