package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "historia_clinica_documento",
       uniqueConstraints = @UniqueConstraint(columnNames = {"historia_id", "documento_id"}),
       indexes = {
           @Index(name = "idx_hist_doc_historia", columnList = "historia_id"),
           @Index(name = "idx_hist_doc_documento", columnList = "documento_id")
       })
public class historia_clinica_documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_id", nullable = false)
    private historia_clinica historiaClinica;

    @Column(name = "DOCUMENTO_ID", nullable = false, columnDefinition = "UUID")
    private UUID documentoId;

    @Column(name = "TENANT_ID", columnDefinition = "UUID", nullable = false)
    private UUID tenantId;

    @Column(name = "FEC_REGISTRO", nullable = false)
    private LocalDateTime fecRegistro = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public historia_clinica getHistoriaClinica() {
        return historiaClinica;
    }

    public void setHistoriaClinica(historia_clinica historiaClinica) {
        this.historiaClinica = historiaClinica;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getFecRegistro() {
        return fecRegistro;
    }

    public void setFecRegistro(LocalDateTime fecRegistro) {
        this.fecRegistro = fecRegistro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof historia_clinica_documento that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
