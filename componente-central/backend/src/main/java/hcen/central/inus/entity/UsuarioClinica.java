package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad que representa la relación entre un usuario de salud y una clínica.
 * Tabla intermedia para la relación ManyToMany entre usuarios y clínicas.
 */
@Entity
@Table(name = "usuario_clinica",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_cedula", "tenant_id"}),
       indexes = {
           @Index(name = "idx_usuario_clinica_cedula", columnList = "usuario_cedula"),
           @Index(name = "idx_usuario_clinica_tenant", columnList = "tenant_id")
       })
public class UsuarioClinica implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_clinica_seq")
    @SequenceGenerator(name = "usuario_clinica_seq", sequenceName = "usuario_clinica_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "usuario_cedula", nullable = false, length = 20)
    private String usuarioCedula;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private java.util.UUID tenantId;

    @Column(name = "fecha_asociacion", nullable = false, updatable = false)
    private Instant fechaAsociacion;

    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        fechaAsociacion = Instant.now();
    }

    // Constructores
    public UsuarioClinica() {}

    public UsuarioClinica(String usuarioCedula, java.util.UUID tenantId) {
        this.usuarioCedula = usuarioCedula;
        this.tenantId = tenantId;
        this.active = true;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuarioCedula() {
        return usuarioCedula;
    }

    public void setUsuarioCedula(String usuarioCedula) {
        this.usuarioCedula = usuarioCedula;
    }

    public java.util.UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(java.util.UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getFechaAsociacion() {
        return fechaAsociacion;
    }

    public void setFechaAsociacion(Instant fechaAsociacion) {
        this.fechaAsociacion = fechaAsociacion;
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
        UsuarioClinica that = (UsuarioClinica) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(usuarioCedula, that.usuarioCedula) &&
               Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioCedula, tenantId);
    }

    @Override
    public String toString() {
        return "UsuarioClinica{" +
                "id=" + id +
                ", usuarioCedula='" + usuarioCedula + '\'' +
                ", tenantId=" + tenantId +
                ", fechaAsociacion=" + fechaAsociacion +
                ", active=" + active +
                '}';
    }
}
