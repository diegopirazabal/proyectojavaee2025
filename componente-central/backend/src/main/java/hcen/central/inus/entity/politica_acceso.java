package hcen.central.inus.entity;

import hcen.central.inus.enums.EstadoPermiso;
import hcen.central.inus.enums.TipoPermiso;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una política de acceso a un documento clínico.
 * Un usuario/paciente puede otorgar permisos temporales a profesionales de salud
 * para acceder a documentos específicos de su historia clínica.
 */
@Entity
@Table(name = "POLITICA_ACCESO")
public class politica_acceso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    /**
     * Historia clínica del paciente que otorga el permiso
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HISTORIA_CLINICA_ID", nullable = false)
    private historia_clinica historiaClinica;

    /**
     * UUID del documento clínico específico al que se otorga acceso
     */
    @Column(name = "DOCUMENTO_ID", columnDefinition = "UUID", nullable = false)
    private UUID documentoId;

    /**
     * Tipo de permiso: específico para un profesional, por especialidad, o por clínica
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_PERMISO", length = 30, nullable = false)
    private TipoPermiso tipoPermiso;

    /**
     * Cédula del profesional específico (solo si tipoPermiso = PROFESIONAL_ESPECIFICO)
     */
    @Column(name = "CI_PROFESIONAL")
    private Integer ciProfesional;

    /**
     * UUID de la clínica del profesional solicitante (tenant_id en componente periférico)
     */
    @Column(name = "TENANT_ID", columnDefinition = "UUID", nullable = false)
    private UUID tenantId;

    /**
     * Especialidad médica (solo si tipoPermiso = POR_ESPECIALIDAD)
     */
    @Column(name = "ESPECIALIDAD", length = 100)
    private String especialidad;

    /**
     * Fecha y hora en que se otorgó el permiso
     */
    @Column(name = "FECHA_OTORGAMIENTO", nullable = false)
    private LocalDateTime fechaOtorgamiento;

    /**
     * Fecha y hora de expiración del permiso (por defecto +15 días desde otorgamiento)
     */
    @Column(name = "FECHA_EXPIRACION", nullable = false)
    private LocalDateTime fechaExpiracion;

    /**
     * Estado actual del permiso
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20, nullable = false)
    private EstadoPermiso estado;

    /**
     * Motivo por el cual se revocó el permiso (solo si estado = REVOCADO)
     */
    @Column(name = "MOTIVO_REVOCACION", length = 255)
    private String motivoRevocacion;

    /**
     * Fecha y hora en que se revocó el permiso
     */
    @Column(name = "FECHA_REVOCACION")
    private LocalDateTime fechaRevocacion;

    // Constructores

    public politica_acceso() {
        this.fechaOtorgamiento = LocalDateTime.now();
        this.estado = EstadoPermiso.ACTIVO;
    }

    // Getters y Setters

    @PrePersist
    protected void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public TipoPermiso getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(TipoPermiso tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public Integer getCiProfesional() {
        return ciProfesional;
    }

    public void setCiProfesional(Integer ciProfesional) {
        this.ciProfesional = ciProfesional;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public LocalDateTime getFechaOtorgamiento() {
        return fechaOtorgamiento;
    }

    public void setFechaOtorgamiento(LocalDateTime fechaOtorgamiento) {
        this.fechaOtorgamiento = fechaOtorgamiento;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public EstadoPermiso getEstado() {
        return estado;
    }

    public void setEstado(EstadoPermiso estado) {
        this.estado = estado;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public void setMotivoRevocacion(String motivoRevocacion) {
        this.motivoRevocacion = motivoRevocacion;
    }

    public LocalDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(LocalDateTime fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }

    // Métodos de utilidad

    /**
     * Verifica si el permiso está activo y no ha expirado
     */
    public boolean estaActivo() {
        return EstadoPermiso.ACTIVO.equals(this.estado) &&
               this.fechaExpiracion != null &&
               this.fechaExpiracion.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si el permiso ha expirado por tiempo
     */
    public boolean estaExpirado() {
        return this.fechaExpiracion != null &&
               this.fechaExpiracion.isBefore(LocalDateTime.now());
    }

    /**
     * Verifica si el permiso fue revocado por el paciente
     */
    public boolean estaRevocado() {
        return EstadoPermiso.REVOCADO.equals(this.estado);
    }

    /**
     * Verifica si el permiso es para un profesional específico
     */
    public boolean esProfesionalEspecifico() {
        return TipoPermiso.PROFESIONAL_ESPECIFICO.equals(this.tipoPermiso);
    }

    /**
     * Verifica si el permiso es por especialidad
     */
    public boolean esPorEspecialidad() {
        return TipoPermiso.POR_ESPECIALIDAD.equals(this.tipoPermiso);
    }

    /**
     * Verifica si el permiso es para toda la clínica
     */
    public boolean esPorClinica() {
        return TipoPermiso.POR_CLINICA.equals(this.tipoPermiso);
    }

    /**
     * Revoca el permiso antes de su fecha de expiración
     */
    public void revocar(String motivo) {
        this.estado = EstadoPermiso.REVOCADO;
        this.motivoRevocacion = motivo;
        this.fechaRevocacion = LocalDateTime.now();
    }

    /**
     * Marca el permiso como expirado (llamado automáticamente al alcanzar fecha de expiración)
     */
    public void marcarComoExpirado() {
        if (estaExpirado() && EstadoPermiso.ACTIVO.equals(this.estado)) {
            this.estado = EstadoPermiso.EXPIRADO;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaOtorgamiento == null) {
            this.fechaOtorgamiento = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoPermiso.ACTIVO;
        }
    }
}
