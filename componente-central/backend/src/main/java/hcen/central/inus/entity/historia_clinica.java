package hcen.central.inus.entity;

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

    // Usuario - Referencia a UsuarioSalud local con composite key
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "USUARIO_ID", referencedColumnName = "id", nullable = false),
        @JoinColumn(name = "USUARIO_CEDULA", referencedColumnName = "cedula", nullable = false)
    })
    private UsuarioSalud usuario;

    // NOTA: Documentos clínicos NO se persisten en central
    // Los documentos están en backend periférico y se obtienen vía API REST
    // Ver HistoriaClinicaService para consultar documentos remotamente

    public UUID getId() { return id; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public LocalDateTime getFecActualizacion() { return fecActualizacion; }
    public void setFecActualizacion(LocalDateTime fecActualizacion) { this.fecActualizacion = fecActualizacion; }
    public UsuarioSalud getUsuario() { return usuario; }
    public void setUsuario(UsuarioSalud usuario) { this.usuario = usuario; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof historia_clinica h && Objects.equals(id,h.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
