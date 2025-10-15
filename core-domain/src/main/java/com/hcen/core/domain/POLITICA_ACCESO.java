package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "POLITICA_ACCESO")
public class politica_acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(length = 50)
    private String tipoAcceso;

    @Column(length = 120)
    private String entidadAutorizada;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    @Column(name = "FEC_VENCIMIENTO")
    private LocalDateTime fecVencimiento;

    @Column(length = 30)
    private String estado;

    // Usuario
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_CI", nullable = false)
    private usuario_salud usuario;

    public UUID getId() { return id; }
    public String getTipoAcceso() { return tipoAcceso; }
    public void setTipoAcceso(String tipoAcceso) { this.tipoAcceso = tipoAcceso; }
    public String getEntidadAutorizada() { return entidadAutorizada; }
    public void setEntidadAutorizada(String entidadAutorizada) { this.entidadAutorizada = entidadAutorizada; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public LocalDateTime getFecVencimiento() { return fecVencimiento; }
    public void setFecVencimiento(LocalDateTime fecVencimiento) { this.fecVencimiento = fecVencimiento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public usuario_salud getUsuario() { return usuario; }
    public void setUsuario(usuario_salud usuario) { this.usuario = usuario; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof politica_acceso p && Objects.equals(id,p.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
