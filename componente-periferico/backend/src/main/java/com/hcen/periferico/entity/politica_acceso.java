package com.hcen.periferico.entity;

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

    // Usuario - Referencia a UsuarioSalud local con composite key (cedula, tenantId)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "usuario_ci", referencedColumnName = "cedula", nullable = false),
        @JoinColumn(name = "usuario_tenant_id", referencedColumnName = "tenant_id", nullable = false)
    })
    private UsuarioSalud usuario;

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

    public UsuarioSalud getUsuario() { return usuario; }
    public void setUsuario(UsuarioSalud usuario) { this.usuario = usuario; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof politica_acceso p && Objects.equals(id,p.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
