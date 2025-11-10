package hcen.central.inus.service;

import hcen.central.inus.dto.DocumentoClinicoDTO;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private Optional<DocumentoClinicoDTO> parseDocumento(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject root = reader.readObject();
            DocumentoClinicoDTO dto = new DocumentoClinicoDTO();

            // Campos básicos
            dto.setId(root.getString("id", null));
            dto.setTenantId(root.getString("tenantId", null));
            dto.setFecCreacion(root.getString("fecCreacion", null));
            dto.setFechaInicioDiagnostico(root.getString("fechaInicioDiagnostico", null));

            // Motivo de consulta
            dto.setCodigoMotivoConsulta(root.getString("codigoMotivoConsulta", null));
            dto.setNombreMotivoConsulta(root.getString("nombreMotivoConsulta", null));

            // Profesional
            dto.setNombreCompletoProfesional(root.getString("nombreCompletoProfesional", null));
            dto.setEspecialidadProfesional(root.getString("especialidadProfesional", null));
            if (!root.isNull("profesionalCi") && root.get("profesionalCi") != null) {
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
            dto.setFechaProximaConsulta(root.getString("fechaProximaConsulta", null));
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
}
