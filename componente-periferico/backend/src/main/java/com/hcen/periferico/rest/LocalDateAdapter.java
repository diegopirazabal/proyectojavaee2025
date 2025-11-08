package com.hcen.periferico.rest;

import jakarta.json.bind.adapter.JsonbAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Adaptador para serializar/deserializar LocalDate en formato ISO-8601 (yyyy-MM-dd)
 * para JSON-B (Jakarta JSON Binding)
 */
public class LocalDateAdapter implements JsonbAdapter<LocalDate, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public String adaptToJson(LocalDate date) {
        return date != null ? date.format(FORMATTER) : null;
    }

    @Override
    public LocalDate adaptFromJson(String dateString) {
        return dateString != null && !dateString.isEmpty()
            ? LocalDate.parse(dateString, FORMATTER)
            : null;
    }
}
