package hcen.central.inus.entity;

import hcen.central.inus.enums.PoliticaAlcance;
import hcen.central.inus.enums.PoliticaEstado;
import hcen.central.inus.enums.TipoAcceso;
import hcen.central.inus.enums.TipoEntidad;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "politica_acceso")
public class politica_acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_salud_id")
    private UsuarioSalud usuarioSalud;

    @Column(name = "entidad_autorizada", length = 100)
    private String entidadAutorizada; //entidadAutorizada guarda el identificador de la entidad a la que se le otorga el permiso.

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", length = 20)
    private TipoEntidad tipoEntidad;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "documento_especifico_id", columnDefinition = "UUID")
    private UUID documentoEspecificoId;

    @Column(name = "fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "tipoAcceso", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'LECTURA'")
    private TipoAcceso tipoAcceso;

    @Column(name = "alcance", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'TODOS'")
    private PoliticaAlcance politicaAlcance;

    @Column(name = "estado", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVO'")
    private PoliticaEstado politicaEstado;

    // Constructores
    public politica_acceso() {
        this.fechaCreacion = LocalDateTime.now();
        this.tipoAcceso = TipoAcceso.LECTURA;
        this.politicaAlcance = PoliticaAlcance.TODOS;
        this.politicaEstado = PoliticaEstado.ACTIVO;
        this.tipoEntidad = TipoEntidad.TODOS;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }



    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public TipoAcceso getTipoAcceso() {
        return tipoAcceso;
    }

    public void setTipoAcceso(TipoAcceso tipoAcceso) {
        this.tipoAcceso = tipoAcceso;
    }

    public PoliticaAlcance getPoliticaAlcance() {
        return politicaAlcance;
    }

    public void setPoliticaAlcance(PoliticaAlcance politicaAlcance) {
        this.politicaAlcance = politicaAlcance;
    }

    public PoliticaEstado getPoliticaEstado() {
        return politicaEstado;
    }

    public void setPoliticaEstado(PoliticaEstado politicaEstado) {
        this.politicaEstado = politicaEstado;
    }

    public UsuarioSalud getUsuarioSalud() {
        return usuarioSalud;
    }

    public void setUsuarioSalud(UsuarioSalud usuarioSalud) {
        this.usuarioSalud = usuarioSalud;
    }

    public String getEntidadAutorizada() {
        return entidadAutorizada;
    }

    public void setEntidadAutorizada(String entidadAutorizada) {
        this.entidadAutorizada = entidadAutorizada;
    }

    public TipoEntidad getTipoEntidad() {
        return tipoEntidad;
    }

    public void setTipoEntidad(TipoEntidad tipoEntidad) {
        this.tipoEntidad = tipoEntidad;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public UUID getDocumentoEspecificoId() {
        return documentoEspecificoId;
    }

    public void setDocumentoEspecificoId(UUID documentoEspecificoId) {
        this.documentoEspecificoId = documentoEspecificoId;
    }

    // Métodos de utilidad
    public boolean estaActiva() {
        return PoliticaEstado.ACTIVO.equals(this.politicaEstado);
    }

    public void activarPolitica() {
        this.politicaEstado = PoliticaEstado.ACTIVO;
    }

    public void desactivarPolitica() {
        this.politicaEstado = PoliticaEstado.INACTIVO;
    }

    public boolean tieneAccesoEscritura() {
        return TipoAcceso.ESCRITURA.equals(this.tipoAcceso) ||
                TipoAcceso.TODOS.equals(this.tipoAcceso);
    }

    public boolean tieneAccesoLectura() {
        return TipoAcceso.LECTURA.equals(this.tipoAcceso) ||
                TipoAcceso.TODOS.equals(this.tipoAcceso);
    }

    public boolean esParaProfesional() {
        return TipoEntidad.PROFESIONAL.equals(this.tipoEntidad);
    }

    public boolean esParaClinica() {
        return TipoEntidad.CLINICA.equals(this.tipoEntidad);
    }

    public boolean esParaEspecialidad() {
        return TipoEntidad.ESPECIALIDAD.equals(this.tipoEntidad);
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }

}
