package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad que representa la relación entre un usuario de salud y una clínica.
 * Tabla intermedia para la relación ManyToMany entre usuarios y clínicas.
 */
@Entity
@Table(name = "usuario_clinica",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_cedula", "clinica_rut"}),
       indexes = {
           @Index(name = "idx_usuario_clinica_cedula", columnList = "usuario_cedula"),
           @Index(name = "idx_usuario_clinica_rut", columnList = "clinica_rut")
       })
public class UsuarioClinica implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_clinica_seq")
    @SequenceGenerator(name = "usuario_clinica_seq", sequenceName = "usuario_clinica_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "usuario_cedula", nullable = false, length = 20)
    private String usuarioCedula;

    @Column(name = "clinica_rut", nullable = false, length = 20)
    private String clinicaRut;

    @Column(name = "fecha_asociacion", nullable = false, updatable = false)
    private Instant fechaAsociacion;

    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        fechaAsociacion = Instant.now();
    }

    // Constructores
    public UsuarioClinica() {}

    public UsuarioClinica(String usuarioCedula, String clinicaRut) {
        this.usuarioCedula = usuarioCedula;
        this.clinicaRut = clinicaRut;
        this.active = true;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuarioCedula() {
        return usuarioCedula;
    }

    public void setUsuarioCedula(String usuarioCedula) {
        this.usuarioCedula = usuarioCedula;
    }

    public String getClinicaRut() {
        return clinicaRut;
    }

    public void setClinicaRut(String clinicaRut) {
        this.clinicaRut = clinicaRut;
    }

    public Instant getFechaAsociacion() {
        return fechaAsociacion;
    }

    public void setFechaAsociacion(Instant fechaAsociacion) {
        this.fechaAsociacion = fechaAsociacion;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioClinica that = (UsuarioClinica) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(usuarioCedula, that.usuarioCedula) &&
               Objects.equals(clinicaRut, that.clinicaRut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioCedula, clinicaRut);
    }

    @Override
    public String toString() {
        return "UsuarioClinica{" +
                "id=" + id +
                ", usuarioCedula='" + usuarioCedula + '\'' +
                ", clinicaRut='" + clinicaRut + '\'' +
                ", fechaAsociacion=" + fechaAsociacion +
                ", active=" + active +
                '}';
    }
}
