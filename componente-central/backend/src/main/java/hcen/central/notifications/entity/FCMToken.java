package hcen.central.notifications.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity para almacenar tokens FCM de dispositivos móviles
 * Asociados a usuarios autenticados para recibir notificaciones push
 */
@Entity
@Table(name = "fcm_tokens",
       indexes = {
           @Index(name = "idx_fcm_usuario_id", columnList = "usuario_id"),
           @Index(name = "idx_fcm_token", columnList = "fcm_token")
       })
public class FCMToken implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * ID del usuario asociado al token
     * Se corresponde con el ID de la tabla usuario_salud
     */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /**
     * Token FCM del dispositivo
     */
    @Column(name = "fcm_token", nullable = false, unique = true, length = 255)
    private String fcmToken;

    /**
     * Identificador único del dispositivo
     */
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    /**
     * Modelo del dispositivo (opcional)
     */
    @Column(name = "device_model", length = 100)
    private String deviceModel;

    /**
     * Versión del sistema operativo (opcional)
     */
    @Column(name = "os_version", length = 50)
    private String osVersion;

    /**
     * Fecha de creación del registro
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Fecha de última actualización
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Indica si el token está activo
     */
    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructores

    public FCMToken() {}

    public FCMToken(Long usuarioId, String fcmToken, String deviceId) {
        this.usuarioId = usuarioId;
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FCMToken fcmToken = (FCMToken) o;
        return Objects.equals(id, fcmToken.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FCMToken{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", deviceId='" + deviceId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", active=" + active +
                '}';
    }
}
