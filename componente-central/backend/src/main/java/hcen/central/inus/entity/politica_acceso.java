package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Política de acceso asignada a un usuario de salud.
 * Cada fila referencia al usuario mediante la PK compuesta (id, cedula).
 */
@Entity
@Table(name = "politica_acceso")
public class politica_acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "tipo_acceso", length = 50)
    private String tipoAcceso;

    @Column(name = "entidad_autorizada", length = 120)
    private String entidadAutorizada;

    @Column(name = "fec_creacion", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    @Column(name = "fec_vencimiento")
    private LocalDateTime fecVencimiento;

    @Column(name = "estado", length = 30)
    private String estado;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "usuario_salud_id", referencedColumnName = "id", nullable = false),
        @JoinColumn(name = "usuario_salud_cedula", referencedColumnName = "cedula", nullable = false)
    })
    private UsuarioSalud usuario;

    public UUID getId() {
        return id;
    }

    public String getTipoAcceso() {
        return tipoAcceso;
    }

    public void setTipoAcceso(String tipoAcceso) {
        this.tipoAcceso = tipoAcceso;
    }

    public String getEntidadAutorizada() {
        return entidadAutorizada;
    }

    public void setEntidadAutorizada(String entidadAutorizada) {
        this.entidadAutorizada = entidadAutorizada;
    }

    public LocalDateTime getFecCreacion() {
        return fecCreacion;
    }

    public void setFecCreacion(LocalDateTime fecCreacion) {
        this.fecCreacion = fecCreacion;
    }

    public LocalDateTime getFecVencimiento() {
        return fecVencimiento;
    }

    public void setFecVencimiento(LocalDateTime fecVencimiento) {
        this.fecVencimiento = fecVencimiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public UsuarioSalud getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioSalud usuario) {
        this.usuario = usuario;
    }

    @PrePersist
    protected void onCreate() {
        if (fecCreacion == null) {
            fecCreacion = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof politica_acceso that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
