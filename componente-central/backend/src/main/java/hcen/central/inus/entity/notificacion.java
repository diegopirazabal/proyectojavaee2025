package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICACION")
public class notificacion {

    @Id
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

    @Column(name = "DATOS_ADICIONALES", columnDefinition = "TEXT")
    private String datosAdicionales;

    // Usuario FK not null - Referencia a UsuarioSalud local con composite key
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns(
        value = {
            @JoinColumn(name = "USUARIO_ID", referencedColumnName = "id", nullable = false),
            @JoinColumn(name = "USUARIO_CEDULA", referencedColumnName = "cedula", nullable = false)
        },
        foreignKey = @ForeignKey(
            name = "fk_notificacion_usuario_salud",
            foreignKeyDefinition = "FOREIGN KEY (USUARIO_ID, USUARIO_CEDULA) REFERENCES usuario_salud(id, cedula)"
        )
    )
    private UsuarioSalud usuario;

    @PrePersist
    protected void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDatosAdicionales() { return datosAdicionales; }
    public void setDatosAdicionales(String datosAdicionales) { this.datosAdicionales = datosAdicionales; }
    public UsuarioSalud getUsuario() { return usuario; }
    public void setUsuario(UsuarioSalud usuario) { this.usuario = usuario; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof notificacion n && Objects.equals(id,n.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
