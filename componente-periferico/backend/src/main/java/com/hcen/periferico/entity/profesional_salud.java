package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "PROFESIONAL_SALUD",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ci", "tenant_id"}),
        @UniqueConstraint(columnNames = {"email", "tenant_id"})
    }
)
public class profesional_salud {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "CI", nullable = false)
    private Integer ci;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    // Relación con Especialidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", referencedColumnName = "ID", insertable = false, updatable = false)
    private Especialidad especialidad;

    @Column(name = "especialidad_id", columnDefinition = "UUID")
    private UUID especialidadId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "PASSWORD", length = 200, nullable = false)
    private String password;

    @Column(name = "ACTIVE", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    // Documentos firmados
    @OneToMany(mappedBy = "profesionalFirmante")
    private List<documento_clinico> documentosFirmados = new ArrayList<>();

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Integer getCi() { return ci; }
    public void setCi(Integer ci) { this.ci = ci; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }

    public UUID getEspecialidadId() { return especialidadId; }
    public void setEspecialidadId(UUID especialidadId) { this.especialidadId = especialidadId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<documento_clinico> getDocumentosFirmados() { return documentosFirmados; }
    public void setDocumentosFirmados(List<documento_clinico> documentosFirmados) {
        this.documentosFirmados = documentosFirmados;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        profesional_salud that = (profesional_salud) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
