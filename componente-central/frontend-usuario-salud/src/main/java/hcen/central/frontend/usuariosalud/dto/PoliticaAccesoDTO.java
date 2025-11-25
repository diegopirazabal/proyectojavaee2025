package hcen.central.frontend.usuariosalud.dto;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.Serializable;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO para representar una política de acceso a un documento clínico.
 * Usado por los usuarios salud para gestionar sus permisos.
 */
public class PoliticaAccesoDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter INPUT_DATETIME =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter INPUT_DATE =
            DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter OUTPUT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String id;
    private String historiaClinicaId;
    private String documentoId;
    private String motivoDocumento;
    private String nombreClinica;
    private String tipoPermiso; // PROFESIONAL_ESPECIFICO, POR_ESPECIALIDAD, POR_CLINICA
    private Integer ciProfesional;
    private String tenantId;
    private String especialidad;
    private String fechaOtorgamiento;
    private String fechaExpiracion;
    private String fechaDocumento;
    private String fechaRegistroDocumento;
    private String estado; // ACTIVO, REVOCADO, EXPIRADO
    private String motivoRevocacion;
    private String fechaRevocacion;

    // Getters y Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHistoriaClinicaId() {
        return historiaClinicaId;
    }

    public void setHistoriaClinicaId(String historiaClinicaId) {
        this.historiaClinicaId = historiaClinicaId;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

    public String getMotivoDocumento() {
        return motivoDocumento;
    }

    public void setMotivoDocumento(String motivoDocumento) {
        this.motivoDocumento = motivoDocumento;
    }

    public String getNombreClinica() {
        return nombreClinica;
    }

    public void setNombreClinica(String nombreClinica) {
        this.nombreClinica = nombreClinica;
    }

    public String getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(String tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public Integer getCiProfesional() {
        return ciProfesional;
    }

    public void setCiProfesional(Integer ciProfesional) {
        this.ciProfesional = ciProfesional;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getFechaOtorgamiento() {
        return fechaOtorgamiento;
    }

    public void setFechaOtorgamiento(String fechaOtorgamiento) {
        this.fechaOtorgamiento = fechaOtorgamiento;
    }

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(String fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getFechaDocumento() {
        return fechaDocumento;
    }

    public void setFechaDocumento(String fechaDocumento) {
        this.fechaDocumento = fechaDocumento;
    }

    public String getFechaRegistroDocumento() {
        return fechaRegistroDocumento;
    }

    public void setFechaRegistroDocumento(String fechaRegistroDocumento) {
        this.fechaRegistroDocumento = fechaRegistroDocumento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public void setMotivoRevocacion(String motivoRevocacion) {
        this.motivoRevocacion = motivoRevocacion;
    }

    public String getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(String fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }

    // Métodos helper para la UI

    /**
     * Retorna una descripción legible del permiso
     */
    public String getDescripcionPermiso() {
        if (tipoPermiso == null) {
            return "Tipo desconocido";
        }

        switch (tipoPermiso) {
            case "PROFESIONAL_ESPECIFICO":
                return "Profesional CI: " + (ciProfesional != null ? ciProfesional : "?");
            case "POR_ESPECIALIDAD":
                return "Especialidad: " + (especialidad != null ? especialidad : "?");
            case "POR_CLINICA":
                return "Toda la clínica";
            default:
                return "Tipo: " + tipoPermiso;
        }
    }

    /**
     * Verifica si el permiso está activo
     */
    public boolean isActivo() {
        return "ACTIVO".equals(estado);
    }

    /**
     * Verifica si el permiso está revocado
     */
    public boolean isRevocado() {
        return "REVOCADO".equals(estado);
    }

    /**
     * Verifica si el permiso está expirado
     */
    public boolean isExpirado() {
        return "EXPIRADO".equals(estado);
    }

    /**
     * Retorna el ID truncado para mostrar en UI
     */
    public String getIdCorto() {
        if (id != null && id.length() > 8) {
            return id.substring(0, 8) + "...";
        }
        return id;
    }

    /**
     * Retorna el ID del documento truncado
     */
    public String getDocumentoDescripcion() {
        if (motivoDocumento != null && !motivoDocumento.isBlank()) {
            return motivoDocumento;
        }
        return documentoId;
    }

    public String getFechaOtorgamientoFormateada() {
        return formatearFecha(fechaOtorgamiento);
    }

    public String getFechaCreacionDocumentoFormateada() {
        return formatearFecha(fechaRegistroDocumento);
    }

    public String getFechaDocumentoFormateada() {
        return formatearFecha(fechaDocumento);
    }

    private String formatearFecha(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        LocalDateTime dateTime = parseDateTime(raw);
        if (dateTime != null) {
            return OUTPUT.format(dateTime.toLocalDate());
        }
        LocalDate dateOnly = parseDate(raw);
        if (dateOnly != null) {
            return OUTPUT.format(dateOnly);
        }
        if (raw.matches("\\d+")) {
            try {
                long value = Long.parseLong(raw);
                if (raw.length() <= 10) {
                    value = value * 1000;
                }
                dateTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(value),
                        java.time.ZoneId.systemDefault());
                return OUTPUT.format(dateTime.toLocalDate());
            } catch (Exception ignored) {
            }
        }
        return raw;
    }

    private LocalDateTime parseDateTime(String raw) {
        try {
            return LocalDateTime.parse(raw, INPUT_DATETIME);
        } catch (Exception ignored) {
        }
        LocalDateTime arrayParsed = parseArrayDateTime(raw);
        if (arrayParsed != null) {
            return arrayParsed;
        }
        LocalDateTime objectParsed = parseObjectDateTime(raw);
        if (objectParsed != null) {
            return objectParsed;
        }
        return null;
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, INPUT_DATE);
        } catch (Exception ignored) {
        }
        LocalDate arrayParsed = parseArrayDate(raw);
        if (arrayParsed != null) {
            return arrayParsed;
        }
        LocalDate objectParsed = parseObjectDate(raw);
        if (objectParsed != null) {
            return objectParsed;
        }
        return null;
    }

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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime parseObjectDateTime(String raw) {
        JsonObject root = parseJsonObject(raw);
        if (root == null) {
            return null;
        }
        JsonObject dateObj = root.containsKey("date") ? root.getJsonObject("date") : root;
        Integer year = getJsonInt(dateObj, "year");
        Integer month = getJsonInt(dateObj, "month");
        Integer day = getJsonInt(dateObj, "day");
        if (year == null || month == null || day == null) {
            return null;
        }
        JsonObject timeObj = root.containsKey("time") ? root.getJsonObject("time") : root;
        Integer hour = getJsonInt(timeObj, "hour");
        Integer minute = getJsonInt(timeObj, "minute");
        Integer second = getJsonInt(timeObj, "second");
        if (hour == null) {
            return LocalDate.of(year, month, day).atStartOfDay();
        }
        return LocalDateTime.of(
                year,
                month,
                day,
                hour,
                minute != null ? minute : 0,
                second != null ? second : 0
        );
    }

    private LocalDate parseObjectDate(String raw) {
        JsonObject root = parseJsonObject(raw);
        if (root == null) {
            return null;
        }
        JsonObject dateObj = root.containsKey("date") ? root.getJsonObject("date") : root;
        Integer year = getJsonInt(dateObj, "year");
        Integer month = getJsonInt(dateObj, "month");
        Integer day = getJsonInt(dateObj, "day");
        if (year == null || month == null || day == null) {
            return null;
        }
        return LocalDate.of(year, month, day);
    }

    private JsonObject parseJsonObject(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null;
        }
        try (JsonReader reader = Json.createReader(new StringReader(trimmed))) {
            JsonValue value = reader.readValue();
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                return value.asJsonObject();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Integer getJsonInt(JsonObject obj, String key) {
        if (obj == null || !obj.containsKey(key) || obj.isNull(key)) {
            return null;
        }
        JsonValue value = obj.get(key);
        if (value.getValueType() == JsonValue.ValueType.NUMBER) {
            JsonNumber number = obj.getJsonNumber(key);
            return number != null ? number.intValue() : null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
