package hcen.central.inus.entity;

import hcen.central.inus.enums.TipoDocumento;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA Entity for OpenID Connect authenticated users
 * Maps gub.uy users to local database
 */
@Entity
@Table(name = "usuario_salud")
@IdClass(UsuarioSalud.UsuarioSaludId.class)
public class UsuarioSalud {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_salud_seq")
    @SequenceGenerator(name = "usuario_salud_seq", sequenceName = "usuario_salud_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;
    
    @Id
    @Column(name = "cedula", nullable = false, length = 20)
    private String cedula;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'CI'")
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
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "last_login")
    private Instant lastLogin;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public UsuarioSalud() {}
    
    public UsuarioSalud(String cedula, String email, String primerNombre, String primerApellido) {
        this.cedula = cedula;
        this.email = email;
        this.primerNombre = primerNombre;
        this.primerApellido = primerApellido;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    
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
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioSalud usuarioSalud = (UsuarioSalud) o;
        return Objects.equals(id, usuarioSalud.id) && 
               Objects.equals(cedula, usuarioSalud.cedula);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, cedula);
    }
    
    /**
     * Clase interna para la clave compuesta
     */
    public static class UsuarioSaludId implements Serializable {
        private Long id;
        private String cedula;
        
        public UsuarioSaludId() {}
        
        public UsuarioSaludId(Long id, String cedula) {
            this.id = id;
            this.cedula = cedula;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UsuarioSaludId that = (UsuarioSaludId) o;
            return Objects.equals(id, that.id) && 
                   Objects.equals(cedula, that.cedula);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id, cedula);
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
