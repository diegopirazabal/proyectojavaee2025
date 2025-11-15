package com.hcen.periferico.frontend.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para documento clínico ambulatorio (Frontend).
 * Debe coincidir exactamente con el DTO del backend para serialización JSON.
 */
public class documento_clinico_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Identificación
    private String id;
    private String tenantId;
    private LocalDateTime fecCreacion;

    // Relaciones
    private String usuarioSaludCedula;
    private Integer profesionalCi;

    // Campos adicionales para mostrar información (no se persisten)
    private String nombreCompletoPaciente;
    private String nombreCompletoProfesional;
    private String especialidadProfesional;

    // Motivo de consulta
    private String codigoMotivoConsulta;
    private String nombreMotivoConsulta; // Nombre resuelto desde codiguera

    // Diagnóstico
    private String descripcionDiagnostico;
    private LocalDate fechaInicioDiagnostico;
    private String codigoEstadoProblema;
    private String nombreEstadoProblema; // Nombre resuelto desde codiguera
    private String codigoGradoCerteza;
    private String nombreGradoCerteza; // Nombre resuelto desde codiguera

    // Instrucciones de seguimiento
    private LocalDate fechaProximaConsulta;
    private String descripcionProximaConsulta;
    private String referenciaAlta;

    // Constructores
    public documento_clinico_dto() {
    }

    public documento_clinico_dto(String id, String usuarioSaludCedula, Integer profesionalCi,
                                 String codigoMotivoConsulta, String descripcionDiagnostico,
                                 LocalDate fechaInicioDiagnostico, String codigoGradoCerteza) {
        this.id = id;
        this.usuarioSaludCedula = usuarioSaludCedula;
        this.profesionalCi = profesionalCi;
        this.codigoMotivoConsulta = codigoMotivoConsulta;
        this.descripcionDiagnostico = descripcionDiagnostico;
        this.fechaInicioDiagnostico = fechaInicioDiagnostico;
        this.codigoGradoCerteza = codigoGradoCerteza;
    }

    // Getters y Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getFecCreacion() {
        return fecCreacion;
    }

    public void setFecCreacion(LocalDateTime fecCreacion) {
        this.fecCreacion = fecCreacion;
    }

    public String getUsuarioSaludCedula() {
        return usuarioSaludCedula;
    }

    public void setUsuarioSaludCedula(String usuarioSaludCedula) {
        this.usuarioSaludCedula = usuarioSaludCedula;
    }

    public Integer getProfesionalCi() {
        return profesionalCi;
    }

    public void setProfesionalCi(Integer profesionalCi) {
        this.profesionalCi = profesionalCi;
    }

    public String getNombreCompletoPaciente() {
        return nombreCompletoPaciente;
    }

    public void setNombreCompletoPaciente(String nombreCompletoPaciente) {
        this.nombreCompletoPaciente = nombreCompletoPaciente;
    }

    public String getNombreCompletoProfesional() {
        return nombreCompletoProfesional;
    }

    public void setNombreCompletoProfesional(String nombreCompletoProfesional) {
        this.nombreCompletoProfesional = nombreCompletoProfesional;
    }

    public String getEspecialidadProfesional() {
        return especialidadProfesional;
    }

    public void setEspecialidadProfesional(String especialidadProfesional) {
        this.especialidadProfesional = especialidadProfesional;
    }

    public String getCodigoMotivoConsulta() {
        return codigoMotivoConsulta;
    }

    public void setCodigoMotivoConsulta(String codigoMotivoConsulta) {
        this.codigoMotivoConsulta = codigoMotivoConsulta;
    }

    public String getNombreMotivoConsulta() {
        return nombreMotivoConsulta;
    }

    public void setNombreMotivoConsulta(String nombreMotivoConsulta) {
        this.nombreMotivoConsulta = nombreMotivoConsulta;
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

    public String getNombreEstadoProblema() {
        return nombreEstadoProblema;
    }

    public void setNombreEstadoProblema(String nombreEstadoProblema) {
        this.nombreEstadoProblema = nombreEstadoProblema;
    }

    public String getCodigoGradoCerteza() {
        return codigoGradoCerteza;
    }

    public void setCodigoGradoCerteza(String codigoGradoCerteza) {
        this.codigoGradoCerteza = codigoGradoCerteza;
    }

    public String getNombreGradoCerteza() {
        return nombreGradoCerteza;
    }

    public void setNombreGradoCerteza(String nombreGradoCerteza) {
        this.nombreGradoCerteza = nombreGradoCerteza;
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
}
