package com.example.hcenmobile.util;

import android.text.TextUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilidades para dar formato consistente a fechas recibidas desde los servicios.
 */
public final class DateFormatterUtil {

    private static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateFormatterUtil() {
    }

    /**
     * Intenta formatear una fecha y hora. Si no hay fecha de creación, utiliza la fecha
     * de diagnóstico como fallback. Devuelve "-" si no puede parsear nada.
     */
    public static String formatDateTime(String fechaCreacion, String fechaDiagnostico) {
        String candidate = !TextUtils.isEmpty(fechaCreacion) ? fechaCreacion : fechaDiagnostico;
        if (TextUtils.isEmpty(candidate)) {
            return "-";
        }

        LocalDateTime dateTime = parseToLocalDateTime(candidate);
        if (dateTime != null) {
            return OUTPUT_FORMATTER.format(dateTime);
        }

        LocalDate date = parseToLocalDate(candidate);
        if (date != null) {
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        return candidate;
    }

    private static LocalDateTime parseToLocalDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Ignoramos y probamos el siguiente formato
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static LocalDate parseToLocalDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
