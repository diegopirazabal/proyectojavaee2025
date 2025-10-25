package hcen.central.inus.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import hcen.central.inus.util.TipoDocumentoMapper;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Deserializador personalizado para tipo_documento de gub.uy
 * gub.uy puede enviar tipo_documento como:
 * - String simple: "CI"
 * - Objeto: {"codigo": "1", "descripcion": "CI"}
 */
public class TipoDocumentoDeserializer extends JsonDeserializer<String> {

    private static final Logger LOGGER = Logger.getLogger(TipoDocumentoDeserializer.class.getName());
    
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        String rawValue = null;

        // Si es un string simple, lo retorna directamente
        if (node.isTextual()) {
            rawValue = node.asText();
        }

        // Si es un objeto, intenta extraer el campo "descripcion" o "codigo"
        if (rawValue == null && node.isObject()) {
            LOGGER.fine(() -> "tipo_documento recibido como objeto: " + node.toString());
            // Primero intenta obtener "descripcion"
            if (node.has("descripcion")) {
                rawValue = node.get("descripcion").asText();
            }
            // Si no, intenta "codigo"
            if (node.has("codigo")) {
                String codigo = node.get("codigo").asText();
                LOGGER.fine(() -> "tipo_documento codigo recibido: '" + codigo + "'");
                // Mapeo de códigos comunes a descripciones
                switch (codigo) {
                    case "1":
                        rawValue = "CI";
                        break;
                    case "2":
                        rawValue = "PASAPORTE";
                        break;
                    case "3":
                        rawValue = "DNI";
                        break;
                    default:
                        rawValue = codigo;
                        break;
                }
            }
        }

        if (rawValue == null || rawValue.isBlank()) {
            rawValue = "CI"; // Default si no llega nada útil
        }

        String tipoNormalizado = TipoDocumentoMapper.toEnum(rawValue).name();
        LOGGER.fine(() -> "tipo_documento mapeado a '" + tipoNormalizado + "' desde '" + rawValue + "'");
        return tipoNormalizado;
    }
}
