package hcen.central.inus.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * Deserializador personalizado para tipo_documento de gub.uy
 * gub.uy puede enviar tipo_documento como:
 * - String simple: "CI"
 * - Objeto: {"codigo": "1", "descripcion": "CI"}
 */
public class TipoDocumentoDeserializer extends JsonDeserializer<String> {
    
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // Si es un string simple, lo retorna directamente
        if (node.isTextual()) {
            return node.asText();
        }
        
        // Si es un objeto, intenta extraer el campo "descripcion" o "codigo"
        if (node.isObject()) {
            // Primero intenta obtener "descripcion"
            if (node.has("descripcion")) {
                return node.get("descripcion").asText();
            }
            // Si no, intenta "codigo"
            if (node.has("codigo")) {
                String codigo = node.get("codigo").asText();
                // Mapeo de códigos comunes a descripciones
                switch (codigo) {
                    case "1": return "CI";
                    case "2": return "PASAPORTE";
                    case "3": return "DNI";
                    default: return codigo;
                }
            }
        }
        
        // Si no puede extraer nada, retorna "CI" por defecto
        return "CI";
    }
}
