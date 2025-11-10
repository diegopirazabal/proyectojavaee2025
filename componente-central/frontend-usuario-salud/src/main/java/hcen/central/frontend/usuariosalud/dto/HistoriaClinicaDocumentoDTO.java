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

    /**
     * Formatea la fecha del documento en formato legible: "dd/MM/yyyy HH:mm"
     */
    public String getFechaFormateada() {
        if (fechaDocumento == null || fechaDocumento.isBlank()) {
            return "";
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(fechaDocumento);
            return dateTime.format(FORMATTER);
        } catch (DateTimeParseException e) {
            return fechaDocumento;
        }
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
     * Retorna información de la clínica (tenant) formateada
     */
    public String getClinicaDisplay() {
        if (tenantId == null || tenantId.isBlank()) {
            return "Clínica desconocida";
        }
        return "Clínica ID: " + tenantId.substring(0, Math.min(8, tenantId.length())) + "...";
    }
}
