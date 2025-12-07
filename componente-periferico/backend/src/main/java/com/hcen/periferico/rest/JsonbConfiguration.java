package com.hcen.periferico.rest;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyNamingStrategy;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import java.time.format.DateTimeFormatter;

/**
 * Configurador de JSON-B para JAX-RS.
 * Configura cómo se serializan/deserializan los objetos JSON en los endpoints REST.
 */
@Provider
public class JsonbConfiguration implements ContextResolver<Jsonb> {

    private final Jsonb jsonb;

    public JsonbConfiguration() {
        JsonbConfig config = new JsonbConfig()
            // Formatear fechas LocalDate como "yyyy-MM-dd" (ISO-8601)
            .withDateFormat("yyyy-MM-dd", null)
            // Usar el adaptador para LocalDate que ya creamos
            .withAdapters(new LocalDateAdapter())
            // Forzar encoding UTF-8 para caracteres especiales (tildes, ñ, etc.)
            .withEncoding("UTF-8");

        this.jsonb = JsonbBuilder.create(config);
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}