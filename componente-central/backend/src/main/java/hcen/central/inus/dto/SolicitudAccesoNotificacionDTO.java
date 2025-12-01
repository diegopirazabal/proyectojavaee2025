package hcen.central.inus.dto;

import java.io.Serializable;

/**
 * DTO para enviar notificaciones de solicitud de acceso a documentos clínicos
 * Contiene toda la información necesaria para que el paciente pueda decidir
 * si otorga acceso al profesional solicitante
 */
public class SolicitudAccesoNotificacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ID de la solicitud en el periférico (para callback posterior)
    private String solicitudId;

    // Datos del paciente
    private String cedulaPaciente;

    // Datos del documento solicitado
    private String documentoId;
    private String fechaDocumento;
    private String motivoConsulta;
    private String diagnostico;

    // Datos del profesional solicitante
    private Integer profesionalCi;
    private String profesionalNombre;
    private String especialidad;
    private String especialidadId;  // UUID de la especialidad

    // Datos de la clínica
    private String tenantId;
    private String nombreClinica;

    // Constructores

    public SolicitudAccesoNotificacionDTO() {
    }

    public SolicitudAccesoNotificacionDTO(
            String solicitudId,
            String cedulaPaciente,
            String documentoId,
            String fechaDocumento,
            String motivoConsulta,
            String diagnostico,
            Integer profesionalCi,
            String profesionalNombre,
            String especialidad,
            String especialidadId,
            String tenantId,
            String nombreClinica) {
        this.solicitudId = solicitudId;
        this.cedulaPaciente = cedulaPaciente;
        this.documentoId = documentoId;
        this.fechaDocumento = fechaDocumento;
        this.motivoConsulta = motivoConsulta;
        this.diagnostico = diagnostico;
        this.profesionalCi = profesionalCi;
        this.profesionalNombre = profesionalNombre;
        this.especialidad = especialidad;
        this.especialidadId = especialidadId;
        this.tenantId = tenantId;
        this.nombreClinica = nombreClinica;
    }

    // Getters y Setters

    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public String getCedulaPaciente() {
        return cedulaPaciente;
    }

    public void setCedulaPaciente(String cedulaPaciente) {
        this.cedulaPaciente = cedulaPaciente;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
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

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public Integer getProfesionalCi() {
        return profesionalCi;
    }

    public void setProfesionalCi(Integer profesionalCi) {
        this.profesionalCi = profesionalCi;
    }

    public String getProfesionalNombre() {
        return profesionalNombre;
    }

    public void setProfesionalNombre(String profesionalNombre) {
        this.profesionalNombre = profesionalNombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(String especialidadId) {
        this.especialidadId = especialidadId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNombreClinica() {
        return nombreClinica;
    }

    public void setNombreClinica(String nombreClinica) {
        this.nombreClinica = nombreClinica;
    }

    @Override
    public String toString() {
        return "SolicitudAccesoNotificacionDTO{" +
                "solicitudId='" + solicitudId + '\'' +
                ", cedulaPaciente='" + cedulaPaciente + '\'' +
                ", documentoId='" + documentoId + '\'' +
                ", profesionalNombre='" + profesionalNombre + '\'' +
                ", nombreClinica='" + nombreClinica + '\'' +
                '}';
    }
}
