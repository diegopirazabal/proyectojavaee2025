package hcen.central.inus.service;

import hcen.central.inus.dto.DocumentoClinicoDTO;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonString;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente para consultar documentos clínicos en el componente periférico.
 */
@Stateless
public class PerifericoDocumentosClient {

    private static final Logger LOGGER = Logger.getLogger(PerifericoDocumentosClient.class.getName());

    private static final String PERIPHERAL_ENV_VAR = "HCEN_PERIPHERAL_API_BASE_URL";
    private static final String PERIPHERAL_SYS_PROP = "hcen.peripheralApiBaseUrl";
    private static final String DEFAULT_PERIPHERAL_URL = "https://prestador-salud.up.railway.app/multitenant-api";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(DEFAULT_TIMEOUT)
        .build();

    private final String peripheralBaseUrl = resolvePeripheralUrl();

    public Optional<DocumentoClinicoDTO> obtenerDocumento(UUID documentoId, UUID tenantId) {
        if (documentoId == null || tenantId == null) {
            return Optional.empty();
        }

        try {
            String url = String.format("%s/documentos/%s?tenantId=%s",
                peripheralBaseUrl, documentoId, tenantId);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseDocumento(response.body());
            }

            LOGGER.log(Level.WARNING, "Fallo al obtener documento {0} del periférico. Código {1}, cuerpo: {2}",
                new Object[]{documentoId, response.statusCode(), response.body()});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al consultar documento " + documentoId + " en el periférico", e);
        }
        return Optional.empty();
    }

    /**
     * Obtiene múltiples documentos del periférico en una sola llamada HTTP (batch cross-tenant)
     * Optimización para evitar N+1 queries
     * NO filtra por tenant - retorna documentos de cualquier clínica
     *
     * @param documentoIds Lista de UUIDs de documentos
     * @param tenantId UUID del tenant (DEPRECADO - se ignora, se mantiene por compatibilidad)
     * @return Lista de DocumentoClinicoDTO (puede contener menos elementos si algunos no se encuentran)
     */
    public List<DocumentoClinicoDTO> obtenerDocumentosBatch(List<UUID> documentoIds, UUID tenantId) {
        List<DocumentoClinicoDTO> resultados = new ArrayList<>();

        if (documentoIds == null || documentoIds.isEmpty()) {
            return resultados;
        }

        try {
            // Construir query params con IDs separados por comas
            String idsParam = documentoIds.stream()
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(","));

            // NO enviar tenantId - queremos documentos de todos los tenants
            String url = String.format("%s/documentos?ids=%s",
                peripheralBaseUrl, idsParam);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                resultados = parseDocumentosList(response.body());
                LOGGER.info("Obtenidos " + resultados.size() + " documentos del periférico en batch para tenant " + tenantId);
            } else {
                LOGGER.log(Level.WARNING, "Fallo al obtener documentos batch del periférico. Código {0}, cuerpo: {1}",
                    new Object[]{response.statusCode(), response.body()});
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al consultar documentos batch en el periférico", e);
        }

        return resultados;
    }

    /**
     * Parsea un array JSON de documentos clínicos
     * Maneja tanto arrays como objetos de error
     */
    private List<DocumentoClinicoDTO> parseDocumentosList(String json) {
        List<DocumentoClinicoDTO> documentos = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonValue rootValue = reader.readValue();

            // Verificar si es un objeto (posible error) o un array
            if (rootValue.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject obj = rootValue.asJsonObject();

                // Si tiene campo "error", es un ErrorResponse del periférico
                if (obj.containsKey("error")) {
                    String errorMsg = obj.getString("error", "Error desconocido");
                    LOGGER.log(Level.WARNING, "El periférico retornó un error: {0}", errorMsg);
                } else {
                    // Podría ser un solo documento en formato objeto
                    parseDocumentoFromObject(obj).ifPresent(documentos::add);
                }
                return documentos;
            }

            // Si es un array, procesarlo normalmente
            if (rootValue.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray array = rootValue.asJsonArray();
                for (JsonValue value : array) {
                    if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                        JsonObject obj = value.asJsonObject();
                        parseDocumentoFromObject(obj).ifPresent(documentos::add);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parseando documentos del periférico: " + json, e);
        }
        return documentos;
    }

    /**
     * Parsea un documento desde un JsonObject
     */
    private Optional<DocumentoClinicoDTO> parseDocumentoFromObject(JsonObject root) {
        try {
            DocumentoClinicoDTO dto = new DocumentoClinicoDTO();

            // Campos básicos
            dto.setId(root.getString("id", null));
            dto.setTenantId(root.getString("tenantId", null));
            dto.setFecCreacion(getStringSafe(root, "fecCreacion"));
            dto.setFechaInicioDiagnostico(getStringSafe(root, "fechaInicioDiagnostico"));

            // Motivo de consulta
            dto.setCodigoMotivoConsulta(root.getString("codigoMotivoConsulta", null));
            dto.setNombreMotivoConsulta(root.getString("nombreMotivoConsulta", null));

            // Profesional
            dto.setNombreCompletoProfesional(root.getString("nombreCompletoProfesional", null));
            dto.setEspecialidadProfesional(root.getString("especialidadProfesional", null));
            if (root.containsKey("profesionalCi") && !root.isNull("profesionalCi")) {
                try {
                    dto.setProfesionalCi(root.getInt("profesionalCi"));
                } catch (Exception ignored) {
                    dto.setProfesionalCi(null);
                }
            }

            // Clínica
            dto.setNombreClinica(root.getString("nombreClinica", null));

            // Diagnóstico
            dto.setDescripcionDiagnostico(root.getString("descripcionDiagnostico", null));
            dto.setNombreEstadoProblema(root.getString("nombreEstadoProblema", null));
            dto.setNombreGradoCerteza(root.getString("nombreGradoCerteza", null));

            // Instrucciones de seguimiento
            dto.setFechaProximaConsulta(getStringSafe(root, "fechaProximaConsulta"));
            dto.setDescripcionProximaConsulta(root.getString("descripcionProximaConsulta", null));
            dto.setReferenciaAlta(root.getString("referenciaAlta", null));

            return Optional.of(dto);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parseando documento individual", e);
            return Optional.empty();
        }
    }

    private Optional<DocumentoClinicoDTO> parseDocumento(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject root = reader.readObject();
            DocumentoClinicoDTO dto = new DocumentoClinicoDTO();

            // Campos básicos
            dto.setId(root.getString("id", null));
            dto.setTenantId(root.getString("tenantId", null));
            dto.setFecCreacion(getStringSafe(root, "fecCreacion"));
            dto.setFechaInicioDiagnostico(getStringSafe(root, "fechaInicioDiagnostico"));

            // Motivo de consulta
            dto.setCodigoMotivoConsulta(root.getString("codigoMotivoConsulta", null));
            dto.setNombreMotivoConsulta(root.getString("nombreMotivoConsulta", null));

            // Profesional
            dto.setNombreCompletoProfesional(root.getString("nombreCompletoProfesional", null));
            dto.setEspecialidadProfesional(root.getString("especialidadProfesional", null));
            if (root.containsKey("profesionalCi") && !root.isNull("profesionalCi")) {
                try {
                    dto.setProfesionalCi(root.getInt("profesionalCi"));
                } catch (Exception ignored) {
                    dto.setProfesionalCi(null);
                }
            }

            // Clínica
            String nombreClinica = root.getString("nombreClinica", null);
            dto.setNombreClinica(nombreClinica);
            LOGGER.info("nombreClinica recibido del periférico: '" + nombreClinica + "' para documento: " + dto.getId());

            // Diagnóstico
            dto.setDescripcionDiagnostico(root.getString("descripcionDiagnostico", null));
            dto.setNombreEstadoProblema(root.getString("nombreEstadoProblema", null));
            dto.setNombreGradoCerteza(root.getString("nombreGradoCerteza", null));

            // Instrucciones de seguimiento
            dto.setFechaProximaConsulta(getStringSafe(root, "fechaProximaConsulta"));
            dto.setDescripcionProximaConsulta(root.getString("descripcionProximaConsulta", null));
            dto.setReferenciaAlta(root.getString("referenciaAlta", null));

            return Optional.of(dto);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parseando documento clínico desde periférico", e);
            return Optional.empty();
        }
    }

    private String resolvePeripheralUrl() {
        String url = System.getenv(PERIPHERAL_ENV_VAR);
        if (url == null || url.isBlank()) {
            url = System.getProperty(PERIPHERAL_SYS_PROP);
        }
        if (url == null || url.isBlank()) {
            url = DEFAULT_PERIPHERAL_URL;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Extrae un string del JsonObject tolerando diferentes tipos de fecha (string u objeto con year/month/day)
     */
    private String getStringSafe(JsonObject root, String key) {
        if (root == null || key == null || !root.containsKey(key)) {
            return null;
        }
        try {
            JsonValue value = root.get(key);
            if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
                return null;
            }
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                return ((JsonString) value).getString();
            }
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject obj = value.asJsonObject();
                if (obj.containsKey("year") && obj.containsKey("month") && obj.containsKey("day")) {
                    try {
                        int year = obj.getInt("year");
                        int month = obj.getInt("month");
                        int day = obj.getInt("day");
                        return java.time.LocalDate.of(year, month, day).toString();
                    } catch (Exception ignored) {
                        // Si falla, devolvemos el JSON como string
                    }
                }
                return obj.toString();
            }
            return value.toString();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo extraer campo " + key + " como String", e);
            return null;
        }
    }
}
