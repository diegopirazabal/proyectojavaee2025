package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

@Entity
@Table(name = "ADMINISTRADOR_HCEN")
public class AdministradorHcen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(length = 80, nullable = false)
    private String usuario;

    @Column(length = 150)
    private String email;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    // Clínicas que gestiona
    @ManyToMany
    @JoinTable(name = "ADMINISTRADOR_CLINICA",
            joinColumns = @JoinColumn(name = "ADMIN_ID"),
            inverseJoinColumns = @JoinColumn(name = "CLINICA_ID"))
    private Set<Clinica> clinicasGestionadas = new HashSet<>();

    public UUID getId() { return id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public Set<Clinica> getClinicasGestionadas() { return clinicasGestionadas; }
    public void setClinicasGestionadas(Set<Clinica> clinicasGestionadas) { this.clinicasGestionadas = clinicasGestionadas; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof AdministradorHcen a && Objects.equals(id,a.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
