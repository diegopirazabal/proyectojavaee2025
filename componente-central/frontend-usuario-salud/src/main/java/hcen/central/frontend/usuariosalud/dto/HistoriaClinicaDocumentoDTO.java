package hcen.central.frontend.usuariosalud.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * DTO para documentos de historia clínica consumidos desde el backend central.
 * Mapea la respuesta del endpoint GET /api/historia-clinica/{cedula}/documentos
 */
public class HistoriaClinicaDocumentoDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());

    private String historiaId;
    private String documentoId;
    private String tenantId;
    private String usuarioCedula;
    private String fechaRegistro;   // ISO-8601
    private String fechaDocumento;  // ISO-8601
    private String motivoConsulta;
    private String profesional;
    private String nombreClinica;

    // Diagnóstico
    private String descripcionDiagnostico;
    private String fechaInicioDiagnostico;  // ISO-8601 o LocalDate
    private String nombreEstadoProblema;
    private String nombreGradoCerteza;

    // Instrucciones de seguimiento
    private String fechaProximaConsulta;  // ISO-8601 o LocalDate
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

    /**
     * Formatea la fecha del documento en formato legible.
     */
    public String getFechaFormateada() {
        return formatDate(fechaDocumento);
    }

    public String getFechaRegistroFormateada() {
        return formatDate(fechaRegistro);
    }

    /**
     * Retorna el motivo de consulta o un mensaje por defecto si no está disponible
     */
    public String getMotivoDisplay() {
        if (motivoConsulta == null || motivoConsulta.isBlank()) {
            return "Motivo no disponible";
        }
        return motivoConsulta;
    }

    /**
     * Retorna el nombre del profesional o un mensaje por defecto si no está disponible
     */
    public String getProfesionalDisplay() {
        if (profesional == null || profesional.isBlank()) {
            return "Profesional sin identificar";
        }
        return profesional;
    }

    /**
     * Retorna el nombre de la clínica o un mensaje por defecto
     */
    public String getClinicaDisplay() {
        if (nombreClinica != null && !nombreClinica.isBlank()) {
            return nombreClinica;
        }
        return "Clínica no especificada";
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(raw);
            return dateTime.format(FORMATTER);
        } catch (DateTimeParseException e) {
            return raw;
        }
    }
}
