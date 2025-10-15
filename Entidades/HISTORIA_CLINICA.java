package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "HISTORIA_CLINICA")
public class HistoriaClinica {

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
    @JoinColumn(name = "USUARIO_CI", nullable = false, unique = true)
    private UsuarioSalud usuario;

    // Documentos clínicos
    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoClinico> documentos = new ArrayList<>();

    public UUID getId() { return id; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public LocalDateTime getFecActualizacion() { return fecActualizacion; }
    public void setFecActualizacion(LocalDateTime fecActualizacion) { this.fecActualizacion = fecActualizacion; }
    public UsuarioSalud getUsuario() { return usuario; }
    public void setUsuario(UsuarioSalud usuario) { this.usuario = usuario; }
    public List<DocumentoClinico> getDocumentos() { return documentos; }
    public void setDocumentos(List<DocumentoClinico> documentos) { this.documentos = documentos; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof HistoriaClinica h && Objects.equals(id,h.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
