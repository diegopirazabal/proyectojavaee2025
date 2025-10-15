package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICACION")
public class notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(length = 50)
    private String tipo;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    @Column(length = 30)
    private String estado;

    // Usuario FK not null
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_CI", nullable = false)
    private usuario_salud usuario;

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public usuario_salud getUsuario() { return usuario; }
    public void setUsuario(usuario_salud usuario) { this.usuario = usuario; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof notificacion n && Objects.equals(id,n.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
