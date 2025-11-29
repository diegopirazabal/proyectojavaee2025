package hcen.central.inus.entity;

import hcen.central.inus.entity.converter.UUIDStringConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Versión para tests con conversión UUID->String y generación manual de ID.
 */
@Entity
@Table(name = "historia_clinica_documento",
       uniqueConstraints = @UniqueConstraint(columnNames = {"historia_id", "documento_id"}),
       indexes = {
           @Index(name = "idx_hist_doc_historia", columnList = "historia_id"),
           @Index(name = "idx_hist_doc_documento", columnList = "documento_id")
       })
public class historia_clinica_documento {

    @Id
    @Convert(converter = UUIDStringConverter.class)
    @Column(name = "ID", length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_id", nullable = false)
    private historia_clinica historiaClinica;

    @Convert(converter = UUIDStringConverter.class)
    @Column(name = "DOCUMENTO_ID", nullable = false, length = 36)
    private UUID documentoId;

    @Convert(converter = UUIDStringConverter.class)
    @Column(name = "TENANT_ID", nullable = false, length = 36)
    private UUID tenantId;

    @Column(name = "FEC_REGISTRO", nullable = false)
    private LocalDateTime fecRegistro = LocalDateTime.now();

    public void ensureIdForTests() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

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
