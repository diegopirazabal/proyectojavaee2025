package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

@Entity
@Table(name = "ADMINISTRADOR_HCEN")
public class administrador_hcen {

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

    // NOTA: La relación con clínicas se maneja a través de la tabla administrador_clinica
    // que es una entidad independiente, no una tabla de join
    // Si necesitas acceder a las clínicas, usa un repositorio o servicio

    public UUID getId() { return id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof administrador_hcen a && Objects.equals(id,a.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
