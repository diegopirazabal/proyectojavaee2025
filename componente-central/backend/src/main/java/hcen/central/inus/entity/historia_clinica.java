package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "HISTORIA_CLINICA")
public class historia_clinica {

    @Id
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    @Column(name = "FEC_ACTUALIZACION")
    private LocalDateTime fecActualizacion;

    @Column(name = "USUARIO_CEDULA", nullable = false, length = 20)
    private String usuarioCedula;

    @OneToOne(mappedBy = "historiaClinica")
    private UsuarioSalud usuarioSalud;


    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<historia_clinica_documento> documentos = new ArrayList<>();

    // NOTA: Documentos clínicos NO se persisten en central
    // Los documentos están en backend periférico y se obtienen vía API REST
    // Ver HistoriaClinicaService para consultar documentos remotamente

    @PrePersist
    protected void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public LocalDateTime getFecActualizacion() { return fecActualizacion; }
    public void setFecActualizacion(LocalDateTime fecActualizacion) { this.fecActualizacion = fecActualizacion; }
    public List<historia_clinica_documento> getDocumentos() { return documentos; }
    public void setDocumentos(List<historia_clinica_documento> documentos) { this.documentos = documentos; }

    public String getUsuarioCedula() { return usuarioCedula; }
    public void setUsuarioCedula(String usuarioCedula) { this.usuarioCedula = usuarioCedula; }

    public UsuarioSalud getUsuario() { return usuarioSalud; }
    public void setUsuario(UsuarioSalud usuario) { this.usuarioSalud = usuario; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof historia_clinica h && Objects.equals(id,h.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
