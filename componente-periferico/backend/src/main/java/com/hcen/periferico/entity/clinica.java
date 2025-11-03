package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "CLINICA")
public class clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "TENANT_ID", columnDefinition = "UUID")
    private UUID tenantId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    @Column(length = 150)
    private String email;

    @Column(name = "FEC_REGISTRO", nullable = false)
    private LocalDateTime fecRegistro = LocalDateTime.now();

    @Column(length = 30)
    private String estado;

    // Usuarios (afiliados/atendidos) - Relación con UsuarioSalud local
    @ManyToMany
    @JoinTable(name = "CLINICA_USUARIO",
            joinColumns = @JoinColumn(name = "CLINICA_ID"),
            inverseJoinColumns = {
                @JoinColumn(name = "USUARIO_CEDULA", referencedColumnName = "cedula"),
                @JoinColumn(name = "USUARIO_TENANT", referencedColumnName = "tenant_id")
            })
    private Set<UsuarioSalud> usuarios = new HashSet<>();

    // Profesionales
    @ManyToMany
    @JoinTable(name = "CLINICA_PROFESIONAL",
            joinColumns = @JoinColumn(name = "CLINICA_ID"),
            inverseJoinColumns = @JoinColumn(name = "PROFESIONAL_CI"))
    private Set<profesional_salud> profesionales = new HashSet<>();

    // Relación con administradores eliminada: usar tabla ADMINISTRADOR_CLINICA vía repositorio

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getFecRegistro() { return fecRegistro; }
    public void setFecRegistro(LocalDateTime fecRegistro) { this.fecRegistro = fecRegistro; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Set<UsuarioSalud> getUsuarios() { return usuarios; }
    public void setUsuarios(Set<UsuarioSalud> usuarios) { this.usuarios = usuarios; }
    public Set<profesional_salud> getProfesionales() { return profesionales; }
    public void setProfesionales(Set<profesional_salud> profesionales) { this.profesionales = profesionales; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof clinica c && Objects.equals(tenantId,c.tenantId)); }
    @Override public int hashCode(){ return Objects.hash(tenantId); }
}
