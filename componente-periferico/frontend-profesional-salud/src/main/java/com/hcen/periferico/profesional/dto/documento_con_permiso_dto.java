package com.hcen.periferico.profesional.dto;

/**
 * DTO que extiende documento_clinico_dto agregando información de permisos
 * Indica si el profesional logueado tiene permiso para ver el documento
 */
public class documento_con_permiso_dto extends documento_clinico_dto {

    private static final long serialVersionUID = 1L;

    /**
     * Indica si el profesional actual tiene permiso para acceder a este documento
     */
    private boolean tienePermiso;

    // Constructores

    public documento_con_permiso_dto() {
        super();
        this.tienePermiso = false; // Por defecto sin permiso
    }

    public documento_con_permiso_dto(documento_clinico_dto documento, boolean tienePermiso) {
        // Copiar todos los campos del documento base
        this.setId(documento.getId());
        this.setTenantId(documento.getTenantId());
        this.setUsuarioSaludCedula(documento.getUsuarioSaludCedula());
        this.setCodigoMotivoConsulta(documento.getCodigoMotivoConsulta());
        this.setNombreMotivoConsulta(documento.getNombreMotivoConsulta());
        this.setDescripcionDiagnostico(documento.getDescripcionDiagnostico());
        this.setFechaInicioDiagnostico(documento.getFechaInicioDiagnostico());
        this.setCodigoEstadoProblema(documento.getCodigoEstadoProblema());
        this.setNombreEstadoProblema(documento.getNombreEstadoProblema());
        this.setCodigoGradoCerteza(documento.getCodigoGradoCerteza());
        this.setNombreGradoCerteza(documento.getNombreGradoCerteza());
        this.setFechaProximaConsulta(documento.getFechaProximaConsulta());
        this.setDescripcionProximaConsulta(documento.getDescripcionProximaConsulta());
        this.setReferenciaAlta(documento.getReferenciaAlta());
        this.setFecCreacion(documento.getFecCreacion());
        this.setNombreCompletoProfesional(documento.getNombreCompletoProfesional());
        this.setProfesionalCi(documento.getProfesionalCi());
        this.setEspecialidadProfesional(documento.getEspecialidadProfesional());
        this.setNombreCompletoPaciente(documento.getNombreCompletoPaciente());

        // Setear el permiso
        this.tienePermiso = tienePermiso;
    }

    // Getter y Setter para tienePermiso

    public boolean isTienePermiso() {
        return tienePermiso;
    }

    public void setTienePermiso(boolean tienePermiso) {
        this.tienePermiso = tienePermiso;
    }

    public boolean getTienePermiso() {
        return tienePermiso;
    }
}
