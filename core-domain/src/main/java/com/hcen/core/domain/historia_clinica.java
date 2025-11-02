package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "HISTORIA_CLINICA")
public class historia_clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    @Column(name = "FEC_ACTUALIZACION")
    private LocalDateTime fecActualizacion;

    // Usuario
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "usuario_ci", referencedColumnName = "cedula", nullable = false),
        @JoinColumn(name = "usuario_tenant_id", referencedColumnName = "tenant_id", nullable = false)
    })
    private usuario_salud usuario;

    // Documentos clínicos
    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<documento_clinico> documentos = new ArrayList<>();

    public UUID getId() { return id; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public LocalDateTime getFecActualizacion() { return fecActualizacion; }
    public void setFecActualizacion(LocalDateTime fecActualizacion) { this.fecActualizacion = fecActualizacion; }
    public usuario_salud getUsuario() { return usuario; }
    public void setUsuario(usuario_salud usuario) { this.usuario = usuario; }
    public List<documento_clinico> getDocumentos() { return documentos; }
    public void setDocumentos(List<documento_clinico> documentos) { this.documentos = documentos; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof historia_clinica h && Objects.equals(id,h.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
