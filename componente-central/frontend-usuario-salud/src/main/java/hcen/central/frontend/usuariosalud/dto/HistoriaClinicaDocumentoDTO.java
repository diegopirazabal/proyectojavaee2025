package hcen.central.frontend.usuariosalud.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import jakarta.json.bind.annotation.JsonbProperty;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para documentos de historia clínica consumidos desde el backend central.
 * Mapea la respuesta del endpoint GET /api/historia-clinica/{cedula}/documentos
 */
public class HistoriaClinicaDocumentoDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
    private static final DateTimeFormatter FLEXIBLE_DATE_TIME = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalEnd()
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .toFormatter();

    private String historiaId;
    private String documentoId;
    private String tenantId;
    private String usuarioCedula;
    @JsonbProperty("fechaRegistro")
    @JsonProperty("fechaRegistro")
    private String fechaRegistro;   // ISO-8601
    @JsonbProperty("fechaDocumento")
    @JsonProperty("fechaDocumento")
    private String fechaDocumento;  // ISO-8601
    private String motivoConsulta;
    private String profesional;
    private String nombreClinica;

    // Diagnóstico
    private String descripcionDiagnostico;
    @JsonbProperty("fechaInicioDiagnostico")
    @JsonProperty("fechaInicioDiagnostico")
    private String fechaInicioDiagnostico;  // ISO-8601 o LocalDate
    private String nombreEstadoProblema;
    private String nombreGradoCerteza;

    // Instrucciones de seguimiento
    @JsonbProperty("fechaProximaConsulta")
    @JsonProperty("fechaProximaConsulta")
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

    public String getFechaInicioDiagnosticoFormateada() {
        String formatted = formatDateOnly(fechaInicioDiagnostico);
        if (formatted == null || formatted.isBlank()) {
            // Fallback: usar fechaDocumento si viene sin fecha de inicio separada
            formatted = formatDateOnly(fechaDocumento);
        }
        if (formatted == null || formatted.isBlank()) {
            // Último recurso: la fecha de registro
            formatted = formatDateOnly(fechaRegistro);
        }
        return formatted;
    }

    public String getFechaProximaConsultaFormateada() {
        String formatted = formatDateOnly(fechaProximaConsulta);
        if (formatted == null || formatted.isBlank()) {
            formatted = formatDateOnly(fechaDocumento);
        }
        if (formatted == null || formatted.isBlank()) {
            formatted = formatDateOnly(fechaRegistro);
        }
        return formatted;
    }

    public String getFechaInicioDiagnosticoDisplay() {
        String value = getFechaInicioDiagnosticoFormateada();
        if (value == null || value.isBlank()) {
            value = getFechaFormateada();
        }
        return (value == null || value.isBlank()) ? "No especificado" : value;
    }

    public String getFechaProximaConsultaDisplay() {
        String value = getFechaProximaConsultaFormateada();
        return (value == null || value.isBlank()) ? "No especificado" : value;
    }

    /**
     * Formatea la fecha del documento en formato legible.
     */
    public String getFechaFormateada() {
        String formatted = formatDate(fechaDocumento);
        if (formatted != null && !formatted.isBlank()) {
            return formatted;
        }
        // Fallback al momento de registro en caso de que el periférico no envíe fechaDocumento
        formatted = formatDate(fechaRegistro);
        return formatted != null ? formatted : "";
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
            return null;
        }
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(raw.trim());
            return offsetDateTime.toLocalDate().format(DATE_ONLY_FORMATTER);
        } catch (DateTimeParseException e) {
            // Ignorado: probamos otros formatos abajo
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(raw.trim());
            return dateTime.toLocalDate().format(DATE_ONLY_FORMATTER);
        } catch (DateTimeParseException e) {
            // Ignorado: probamos formato de fecha sin hora
        }
        try {
            LocalDate localDate = LocalDate.parse(raw.trim());
            return localDate.format(DATE_ONLY_FORMATTER);
        } catch (DateTimeParseException e) {
            // Ignorado: probamos formato de array
        }
        // Intentar parsear formato array [yyyy, mm, dd, hh, mm, ss]
        LocalDateTime arrayDateTime = parseArrayDateTime(raw.trim());
        if (arrayDateTime != null) {
            return arrayDateTime.toLocalDate().format(DATE_ONLY_FORMATTER);
        }
        // Intentar parsear formato array de fecha [yyyy, mm, dd]
        LocalDate arrayDate = parseArrayDate(raw.trim());
        if (arrayDate != null) {
            return arrayDate.format(DATE_ONLY_FORMATTER);
        }
        // Último recurso: devolver el raw para no mostrar vacío en la UI
        return raw;
    }

    private String formatDateOnly(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        LocalDate parsed = tryParseLocalDate(raw.trim());
        if (parsed != null) {
            return parsed.format(DATE_ONLY_FORMATTER);
        }
        // Último recurso: devolver el raw para no mostrar vacío en la UI
        return raw;
    }

    private LocalDate tryParseLocalDate(String raw) {
        String value = raw.trim();
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(value);
            return offsetDateTime.toLocalDate();
        } catch (DateTimeParseException e) {
            // Ignorado, probamos otros formatos
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value);
            return dateTime.toLocalDate();
        } catch (DateTimeParseException e) {
            // Ignorado
        }
        try {
            LocalDateTime flexible = LocalDateTime.parse(value, FLEXIBLE_DATE_TIME);
            return flexible.toLocalDate();
        } catch (DateTimeParseException e) {
            // Ignorado
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            // Ignorado, probamos formato array
        }
        // Intentar parsear formato array [yyyy, mm, dd]
        LocalDate arrayDate = parseArrayDate(value);
        if (arrayDate != null) {
            return arrayDate;
        }
        return null;
    }

    /**
     * Parsea una fecha en formato array [yyyy, mm, dd, hh, mm, ss]
     * @param raw String con formato array
     * @return LocalDateTime parseado o null si no es válido
     */
    private LocalDateTime parseArrayDateTime(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }
        String[] parts = trimmed.substring(1, trimmed.length() - 1).split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int day = Integer.parseInt(parts[2].trim());
            int hour = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 0;
            int minute = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 0;
            int second = parts.length > 5 ? Integer.parseInt(parts[5].trim()) : 0;
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parsea una fecha en formato array [yyyy, mm, dd]
     * @param raw String con formato array
     * @return LocalDate parseado o null si no es válido
     */
    private LocalDate parseArrayDate(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }
        String[] parts = trimmed.substring(1, trimmed.length() - 1).split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int day = Integer.parseInt(parts[2].trim());
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
