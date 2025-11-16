package hcen.central.inus.entity;

import hcen.central.inus.enums.TipoDocumento;
import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA Entity for OpenID Connect authenticated users
 * Maps gub.uy users to local database
 */
@Entity
@Table(name = "usuario_salud")
@IdClass(UsuarioSalud.UsuarioSaludId.class)
public class UsuarioSalud {
    
    @Id
    @Column(name = "cedula", nullable = false, length = 20)
    private String cedula;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_salud_seq")
    @SequenceGenerator(name = "usuario_salud_seq", sequenceName = "usuario_salud_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;


    @OneToOne
    @JoinColumn(name = "historia_clinica_id")
    private historia_clinica historiaClinica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'DO'")
    private TipoDocumento tipoDeDocumento;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "email_verificado", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean emailVerificado = true;
    
    @Column(name = "nombre_completo", length = 255)
    private String nombreCompleto;
    
    @Column(name = "primer_nombre", nullable = false, length = 100)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;
    
    @Column(name = "primer_apellido", nullable = false, length = 100)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;

    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;
    
    @Column(name = "updated_at")
    private Timestamp updatedAt;
    
    @Column(name = "last_login")
    private Timestamp lastLogin;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        setCreatedAt(now);
        setUpdatedAt(now);
    }
    
    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(Instant.now());
    }
    
    public UsuarioSalud() {}
    
    public UsuarioSalud(String cedula, String email, String primerNombre, String primerApellido) {
        this.cedula = cedula;
        this.email = email;
        this.primerNombre = primerNombre;
        this.primerApellido = primerApellido;
    }

    public void setHistoriaClinica(historia_clinica historiaClinica) {
        this.historiaClinica = historiaClinica;
        if (historiaClinica != null && historiaClinica.getUsuario() != this) {
            historiaClinica.setUsuario(this);
        }
    }

    public historia_clinica getHistoriaClinica() {
        return historiaClinica;
    }
    
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TipoDocumento getTipoDeDocumento() { return tipoDeDocumento; }
    public void setTipoDeDocumento(TipoDocumento tipoDeDocumento) { this.tipoDeDocumento = tipoDeDocumento; }
    
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Boolean getEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(Boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public Boolean isEmailVerificado() { return emailVerificado; }
    
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }
    
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }
    
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }
    
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean isActive() { return active; }
    
    public Instant getCreatedAt() { return createdAt != null ? createdAt.toInstant() : null; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt != null ? Timestamp.from(createdAt) : null; }
    
    public Instant getUpdatedAt() { return updatedAt != null ? updatedAt.toInstant() : null; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt != null ? Timestamp.from(updatedAt) : null; }
    
    public Instant getLastLogin() { return lastLogin != null ? lastLogin.toInstant() : null; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin != null ? Timestamp.from(lastLogin) : null; }

    public UUID getTenantId() {
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId != null ? tenantId.toString() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioSalud usuarioSalud = (UsuarioSalud) o;
        return Objects.equals(cedula, usuarioSalud.cedula) &&
               Objects.equals(id, usuarioSalud.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cedula, id);
    }
    
    /**
     * Clase interna para la clave compuesta
     */
    public static class UsuarioSaludId implements Serializable {
        private String cedula;
        private Long id;
        
        public UsuarioSaludId() {}
        
        public UsuarioSaludId(String cedula, Long id) {
            this.cedula = cedula;
            this.id = id;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UsuarioSaludId that = (UsuarioSaludId) o;
            return Objects.equals(cedula, that.cedula) &&
                   Objects.equals(id, that.id);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(cedula, id);
        }
    }
    
    @Override
    public String toString() {
        return "UsuarioSalud{" +
                "id=" + id +
                ", cedula='" + cedula + '\'' +
                ", email='" + email + '\'' +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", active=" + active +
                '}';
    }
}
