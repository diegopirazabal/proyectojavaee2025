package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad para documentos clínicos ambulatorios.
 * Representa una consulta no urgente con motivo, diagnóstico e instrucciones de seguimiento.
 * Sigue el estándar de Historia Clínica Electrónica Ambulatoria.
 */
@Entity
@Table(name = "DOCUMENTO_CLINICO", indexes = {
    @Index(name = "idx_documento_tenant", columnList = "TENANT_ID"),
    @Index(name = "idx_documento_paciente", columnList = "USUARIO_SALUD_CEDULA, TENANT_ID"),
    @Index(name = "idx_documento_profesional", columnList = "PROFESIONAL_CI")
})
public class documento_clinico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "TENANT_ID", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    // ============ RELACIONES ============

    // Paciente (UsuarioSalud usa composite key: cedula + tenant_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "USUARIO_SALUD_CEDULA", referencedColumnName = "cedula", insertable = false, updatable = false),
        @JoinColumn(name = "TENANT_ID", referencedColumnName = "tenant_id", insertable = false, updatable = false)
    })
    private UsuarioSalud paciente;

    // Columna para la cédula del paciente (necesaria para la relación con composite key)
    @Column(name = "USUARIO_SALUD_CEDULA", nullable = false, length = 20)
    private String usuarioSaludCedula;

    // Profesional que firma el documento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESIONAL_CI", nullable = false)
    private profesional_salud profesionalFirmante;

    // ============ MOTIVO DE CONSULTA ============

    @Column(name = "CODIGO_MOTIVO_CONSULTA", nullable = false, length = 50)
    private String codigoMotivoConsulta;

    // ============ DIAGNÓSTICO ============

    @Column(name = "DESCRIPCION_DIAGNOSTICO", nullable = false, columnDefinition = "TEXT")
    private String descripcionDiagnostico;

    @Column(name = "FECHA_INICIO_DIAGNOSTICO", nullable = false)
    private LocalDate fechaInicioDiagnostico;

    @Column(name = "CODIGO_ESTADO_PROBLEMA", length = 50)
    private String codigoEstadoProblema;

    @Column(name = "CODIGO_GRADO_CERTEZA", nullable = false, length = 50)
    private String codigoGradoCerteza;

    // ============ INSTRUCCIONES DE SEGUIMIENTO ============

    @Column(name = "FECHA_PROXIMA_CONSULTA")
    private LocalDate fechaProximaConsulta;

    @Column(name = "DESCRIPCION_PROXIMA_CONSULTA", columnDefinition = "TEXT")
    private String descripcionProximaConsulta;

    @Column(name = "REFERENCIA_ALTA", length = 500)
    private String referenciaAlta;

    @Column(name = "HIST_CLINICA_ID", nullable = false, columnDefinition = "UUID")
    private UUID histClinicaId;

    // ============ GETTERS Y SETTERS ============

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getFecCreacion() {
        return fecCreacion;
    }

    public void setFecCreacion(LocalDateTime fecCreacion) {
        this.fecCreacion = fecCreacion;
    }

    public UsuarioSalud getPaciente() {
        return paciente;
    }

    public void setPaciente(UsuarioSalud paciente) {
        this.paciente = paciente;
    }

    public String getUsuarioSaludCedula() {
        return usuarioSaludCedula;
    }

    public void setUsuarioSaludCedula(String usuarioSaludCedula) {
        this.usuarioSaludCedula = usuarioSaludCedula;
    }

    public profesional_salud getProfesionalFirmante() {
        return profesionalFirmante;
    }

    public void setProfesionalFirmante(profesional_salud profesionalFirmante) {
        this.profesionalFirmante = profesionalFirmante;
    }

    public String getCodigoMotivoConsulta() {
        return codigoMotivoConsulta;
    }

    public void setCodigoMotivoConsulta(String codigoMotivoConsulta) {
        this.codigoMotivoConsulta = codigoMotivoConsulta;
    }

    public String getDescripcionDiagnostico() {
        return descripcionDiagnostico;
    }

    public void setDescripcionDiagnostico(String descripcionDiagnostico) {
        this.descripcionDiagnostico = descripcionDiagnostico;
    }

    public LocalDate getFechaInicioDiagnostico() {
        return fechaInicioDiagnostico;
    }

    public void setFechaInicioDiagnostico(LocalDate fechaInicioDiagnostico) {
        this.fechaInicioDiagnostico = fechaInicioDiagnostico;
    }

    public String getCodigoEstadoProblema() {
        return codigoEstadoProblema;
    }

    public void setCodigoEstadoProblema(String codigoEstadoProblema) {
        this.codigoEstadoProblema = codigoEstadoProblema;
    }

    public String getCodigoGradoCerteza() {
        return codigoGradoCerteza;
    }

    public void setCodigoGradoCerteza(String codigoGradoCerteza) {
        this.codigoGradoCerteza = codigoGradoCerteza;
    }

    public LocalDate getFechaProximaConsulta() {
        return fechaProximaConsulta;
    }

    public void setFechaProximaConsulta(LocalDate fechaProximaConsulta) {
        this.fechaProximaConsulta = fechaProximaConsulta;
    }

    public String getDescripcionProximaConsulta() {
        return descripcionProximaConsulta;
    }

    public void setDescripcionProximaConsulta(String descripcionProximaConsulta) {
        this.descripcionProximaConsulta = descripcionProximaConsulta;
    }

    public String getReferenciaAlta() {
        return referenciaAlta;
    }

    public void setReferenciaAlta(String referenciaAlta) {
        this.referenciaAlta = referenciaAlta;
    }

    public UUID getHistClinicaId() {
        return histClinicaId;
    }

    public void setHistClinicaId(UUID histClinicaId) {
        this.histClinicaId = histClinicaId;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof documento_clinico d && Objects.equals(id, d.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
