package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad JPA para sesiones OIDC activas
 * Nota: No se especifican nombres de tablas ni columnas personalizados.
 */
@Entity
public class OIDCSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador de sesión propio del sistema (cookie, header, etc.)
    @Column(nullable = false, unique = true)
    private String sessionId;

    // Subject del usuario autenticado (de gub.uy)
    @Column(nullable = false)
    private String userSub;

    // Tokens (pueden almacenarse encriptados a nivel de aplicación/DB)
    @Lob
    private String accessToken;

    @Lob
    private String refreshToken;

    @Lob
    private String idToken;

    // PKCE (temporal, opcional)
    private String codeVerifier;

    // Fechas y estado
    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant expiresAt;

    private Boolean active = true;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public OIDCSession() {}

    public OIDCSession(String sessionId, String userSub) {
        this.sessionId = sessionId;
        this.userSub = userSub;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserSub() { return userSub; }
    public void setUserSub(String userSub) { this.userSub = userSub; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OIDCSession that = (OIDCSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}
