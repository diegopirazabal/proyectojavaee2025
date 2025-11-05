package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad para almacenar usuarios de salud en el componente periférico.
 * Cada usuario está asociado a una clínica específica mediante tenant_id.
 * Esta es la fuente de verdad local para el componente periférico.
 */
@Entity
@Table(name = "usuario_salud",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cedula", "tenant_id"}),
       indexes = {
           @Index(name = "idx_tenant", columnList = "tenant_id"),
           @Index(name = "idx_sync_pending", columnList = "sincronizado_central")
       })
@IdClass(UsuarioSalud.UsuarioSaludId.class)
public class UsuarioSalud implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cedula", nullable = false, length = 20)
    private String cedula;

    @Id
    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;

    @Column(name = "primer_nombre", length = 50)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "primer_apellido", length = 50)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 50)
    private String segundoApellido;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "fecha_nac")
    private LocalDate fechaNacimiento;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Indica si el usuario ha sido sincronizado con el componente central.
     * false = pendiente de sincronización
     * true = ya sincronizado con central
     */
    @Column(name = "sincronizado_central", nullable = false)
    private Boolean sincronizadoCentral = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (sincronizadoCentral == null) {
            sincronizadoCentral = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructores
    public UsuarioSalud() {
    }

    public UsuarioSalud(String cedula, UUID tenantId) {
        this.cedula = cedula;
        this.tenantId = tenantId;
    }

    // Getters y Setters
    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getSincronizadoCentral() {
        return sincronizadoCentral;
    }

    public void setSincronizadoCentral(Boolean sincronizadoCentral) {
        this.sincronizadoCentral = sincronizadoCentral;
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

    /**
     * Retorna el nombre completo del usuario.
     */
    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (primerNombre != null) sb.append(primerNombre);
        if (segundoNombre != null) sb.append(" ").append(segundoNombre);
        if (primerApellido != null) sb.append(" ").append(primerApellido);
        if (segundoApellido != null) sb.append(" ").append(segundoApellido);
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioSalud that = (UsuarioSalud) o;
        return Objects.equals(cedula, that.cedula) &&
               Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cedula, tenantId);
    }

    @Override
    public String toString() {
        return "UsuarioSalud{" +
                "cedula='" + cedula + '\'' +
                ", tenantId=" + tenantId +
                ", nombreCompleto='" + getNombreCompleto() + '\'' +
                ", email='" + email + '\'' +
                ", sincronizadoCentral=" + sincronizadoCentral +
                '}';
    }

    /**
     * Clase para la composite key (cedula, tenant_id)
     */
    public static class UsuarioSaludId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String cedula;
        private UUID tenantId;

        public UsuarioSaludId() {
        }

        public UsuarioSaludId(String cedula, UUID tenantId) {
            this.cedula = cedula;
            this.tenantId = tenantId;
        }

        public String getCedula() {
            return cedula;
        }

        public void setCedula(String cedula) {
            this.cedula = cedula;
        }

        public UUID getTenantId() {
            return tenantId;
        }

        public void setTenantId(UUID tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UsuarioSaludId that = (UsuarioSaludId) o;
            return Objects.equals(cedula, that.cedula) &&
                   Objects.equals(tenantId, that.tenantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cedula, tenantId);
        }
    }
}
