package hcen.central.inus.dto;

/**
 * DTO expuesto al componente móvil con la información consolidada de un documento clínico.
 */
public class HistoriaClinicaDocumentoDetalleResponse {

    private String historiaId;
    private String documentoId;
    private String tenantId;
    private String usuarioCedula;
    private String fechaRegistro;
    private String fechaDocumento;
    private String motivoConsulta;
    private String profesional;
    private String nombreClinica;

    // Diagnóstico
    private String descripcionDiagnostico;
    private String fechaInicioDiagnostico;
    private String nombreEstadoProblema;
    private String nombreGradoCerteza;

    // Instrucciones de seguimiento
    private String fechaProximaConsulta;
    private String descripcionProximaConsulta;
    private String referenciaAlta;

    public String getHistoriaId() {
        return historiaId;
    }

    public void setHistoriaId(String historiaId) {
        this.historiaId = historiaId;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsuarioCedula() {
        return usuarioCedula;
    }

    public void setUsuarioCedula(String usuarioCedula) {
        this.usuarioCedula = usuarioCedula;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getFechaDocumento() {
        return fechaDocumento;
    }

    public void setFechaDocumento(String fechaDocumento) {
        this.fechaDocumento = fechaDocumento;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String motivoConsulta) {
        this.motivoConsulta = motivoConsulta;
    }

    public String getProfesional() {
        return profesional;
    }

    public void setProfesional(String profesional) {
        this.profesional = profesional;
    }

    public String getNombreClinica() {
        return nombreClinica;
    }

    public void setNombreClinica(String nombreClinica) {
        this.nombreClinica = nombreClinica;
    }

    public String getDescripcionDiagnostico() {
        return descripcionDiagnostico;
    }

    public void setDescripcionDiagnostico(String descripcionDiagnostico) {
        this.descripcionDiagnostico = descripcionDiagnostico;
    }

    public String getFechaInicioDiagnostico() {
        return fechaInicioDiagnostico;
    }

    public void setFechaInicioDiagnostico(String fechaInicioDiagnostico) {
        this.fechaInicioDiagnostico = fechaInicioDiagnostico;
    }

    public String getNombreEstadoProblema() {
        return nombreEstadoProblema;
    }

    public void setNombreEstadoProblema(String nombreEstadoProblema) {
        this.nombreEstadoProblema = nombreEstadoProblema;
    }

    public String getNombreGradoCerteza() {
        return nombreGradoCerteza;
    }

    public void setNombreGradoCerteza(String nombreGradoCerteza) {
        this.nombreGradoCerteza = nombreGradoCerteza;
    }

    public String getFechaProximaConsulta() {
        return fechaProximaConsulta;
    }

    public void setFechaProximaConsulta(String fechaProximaConsulta) {
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
