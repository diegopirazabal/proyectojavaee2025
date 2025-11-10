package hcen.central.inus.dto;

/**
 * DTO que representa la respuesta del componente periférico para un documento clínico.
 */
public class DocumentoClinicoDTO {

    private String id;
    private String tenantId;
    private String fecCreacion;
    private String fechaInicioDiagnostico;
    private String codigoMotivoConsulta;
    private String nombreMotivoConsulta;
    private String nombreCompletoProfesional;
    private Integer profesionalCi;
    private String especialidadProfesional;
    private String nombreClinica;

    // Diagnóstico
    private String descripcionDiagnostico;
    private String nombreEstadoProblema;
    private String nombreGradoCerteza;

    // Instrucciones de seguimiento
    private String fechaProximaConsulta;
    private String descripcionProximaConsulta;
    private String referenciaAlta;

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

    public String getFecCreacion() {
        return fecCreacion;
    }

    public void setFecCreacion(String fecCreacion) {
        this.fecCreacion = fecCreacion;
    }

    public String getFechaInicioDiagnostico() {
        return fechaInicioDiagnostico;
    }

    public void setFechaInicioDiagnostico(String fechaInicioDiagnostico) {
        this.fechaInicioDiagnostico = fechaInicioDiagnostico;
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

    public String getNombreCompletoProfesional() {
        return nombreCompletoProfesional;
    }

    public void setNombreCompletoProfesional(String nombreCompletoProfesional) {
        this.nombreCompletoProfesional = nombreCompletoProfesional;
    }

    public Integer getProfesionalCi() {
        return profesionalCi;
    }

    public void setProfesionalCi(Integer profesionalCi) {
        this.profesionalCi = profesionalCi;
    }

    public String getEspecialidadProfesional() {
        return especialidadProfesional;
    }

    public void setEspecialidadProfesional(String especialidadProfesional) {
        this.especialidadProfesional = especialidadProfesional;
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
