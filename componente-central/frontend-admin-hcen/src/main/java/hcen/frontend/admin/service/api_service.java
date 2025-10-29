package hcen.frontend.admin.service;

import hcen.frontend.admin.dto.admin_hcen_dto;
import hcen.frontend.admin.dto.clinica_dto;
import hcen.frontend.admin.dto.clinica_form;
import hcen.frontend.admin.dto.prestador_dto;
import hcen.frontend.admin.dto.prestador_form;
import hcen.frontend.admin.dto.usuario_salud_dto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class api_service {

    private static final String CENTRAL_ENV_VAR = "HCEN_API_BASE_URL";
    private static final String CENTRAL_SYS_PROP = "hcen.apiBaseUrl";
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080/hcen-central/api";

    private static final String PERIPHERAL_ENV_VAR = "HCEN_PERIPHERAL_API_BASE_URL";
    private static final String PERIPHERAL_SYS_PROP = "hcen.peripheralApiBaseUrl";
    private static final String DEFAULT_PERIPHERAL_URL = "http://localhost:8080/multitenant-api";

    private static final Logger LOGGER = Logger.getLogger(api_service.class.getName());

    private final String backendUrl = resolveBackendUrl();
    private final String peripheralUrl = resolvePeripheralUrl();

    public admin_hcen_dto authenticate(String username, String password) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(backendUrl + "/auth/login");

            JsonObject loginData = Json.createObjectBuilder()
                    .add("username", username)
                    .add("password", password)
                    .build();

            request.setEntity(new StringEntity(loginData.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    return parseAdminFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error authenticating administrator", e);
        }
        return null;
    }

    public boolean triggerBroadcastNotification() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(backendUrl + "/notifications/broadcast-test");
            request.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error triggering broadcast notification", e);
        }
        return false;
    }

    public List<usuario_salud_dto> obtenerUsuariosSalud() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(backendUrl + "/usuarios-salud");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    return parseUsuariosSalud(responseBody);
                }
                throw new IOException("Código inesperado al obtener usuarios de salud: " + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios de salud", e);
            throw new RuntimeException("No se pudo obtener la lista de usuarios de salud", e);
        }
    }

    public boolean enviarNotificacionUsuario(String cedula, String mensaje) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(backendUrl + "/notifications/usuarios/" + cedula);

            JsonObject payload = Json.createObjectBuilder()
                    .add("message", mensaje != null ? mensaje : "mensaje de prueba")
                    .build();
            request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error enviando notificación al usuario " + cedula, e);
            return false;
        }
    }

    public List<prestador_dto> obtenerPrestadores() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(peripheralUrl + "/prestadores");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    return parsePrestadores(responseBody);
                }
                throw new IOException("Código inesperado al obtener prestadores: " + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener prestadores", e);
            throw new RuntimeException("No se pudo obtener la lista de prestadores", e);
        }
    }

    public List<clinica_dto> obtenerClinicas() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(peripheralUrl + "/clinicas");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    return parseClinicas(responseBody);
                }
                throw new IOException("Código inesperado al obtener clínicas: " + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener clínicas", e);
            throw new RuntimeException("No se pudo obtener la lista de clínicas", e);
        }
    }

    public String crearPrestador(prestador_form form) {
        if (form == null) {
            return "Formulario de prestador inválido.";
        }
        if (form.getRut() == null || form.getRut().isBlank()) {
            return "El RUT del prestador es obligatorio.";
        }
        if (form.getNombre() == null || form.getNombre().isBlank()) {
            return "El nombre del prestador es obligatorio.";
        }
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(peripheralUrl + "/prestadores");

            JsonObject payload = Json.createObjectBuilder()
                    .add("rut", form.getRut() == null ? "" : form.getRut())
                    .add("nombre", form.getNombre() == null ? "" : form.getNombre())
                    .add("email", form.getEmail() == null ? "" : form.getEmail())
                    .build();

            request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                if (status == 201 || status == 200) {
                    return null;
                }
                String body = readEntityContent(response);
                LOGGER.warning(() -> "Alta de prestador falló con código " + status + ". Respuesta: " + body);
                String message = extractErrorMessage(body);
                if (message == null || message.isBlank()) {
                    message = "No se pudo crear el prestador. Código HTTP " + status + ".";
                }
                return message;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al crear prestador", e);
            return "Error de comunicación con el nodo periférico.";
        }
    }

    public String crearClinica(clinica_form form) {
        if (form == null) {
            return "Formulario de clínica inválido.";
        }
        if (form.getNombre() == null || form.getNombre().isBlank()) {
            return "El nombre de la clínica es obligatorio.";
        }
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(peripheralUrl + "/clinicas");

            JsonObject payload = Json.createObjectBuilder()
                    .add("nombre", form.getNombre() == null ? "" : form.getNombre())
                    .add("direccion", form.getDireccion() == null ? "" : form.getDireccion())
                    .add("email", form.getEmail() == null ? "" : form.getEmail())
                    .build();

            request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                if (status == 201 || status == 200) {
                    return null;
                }
                String body = readEntityContent(response);
                LOGGER.warning(() -> "Alta de clínica falló con código " + status + ". Respuesta: " + body);
                String message = extractErrorMessage(body);
                if (message == null || message.isBlank()) {
                    message = "No se pudo crear la clínica. Código HTTP " + status + ".";
                }
                return message;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al crear clínica", e);
            return "Error de comunicación con el nodo periférico.";
        }
    }

    public String getOidcLoginUrl() {
        // redirect_uri DEBE ser fija y estar registrada en gub.uy
        ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
        String serverName = external.getRequestServerName();
        
        // Determinar si es producción o desarrollo
        boolean isProduction = "hcen-uy.web.elasticloud.uy".equals(serverName);
        String redirectUri;
        String baseUrl;
        
        if (isProduction) {
            // Producción
            baseUrl = "https://hcen-uy.web.elasticloud.uy";
            redirectUri = "https://hcen-uy.web.elasticloud.uy/api/auth/callback";
        } else {
            // Desarrollo - backend en /hcen-central
            baseUrl = "http://localhost:8080/hcen-central";
            redirectUri = "http://localhost:8080/hcen-central/api/auth/callback";
        }
        
        try {
            return baseUrl + "/api/auth/login?redirect_uri=" + java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&origin=admin";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error encoding redirect_uri", e);
            return baseUrl + "/api/auth/login?redirect_uri=" + redirectUri + "&origin=admin";
        }
    }

    public boolean isBackendAvailable() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(backendUrl + "/health");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private admin_hcen_dto parseAdminFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            admin_hcen_dto admin = new admin_hcen_dto();
            admin.setId(jsonObject.getJsonNumber("id").longValue());
            admin.setUsername(jsonObject.getString("username"));
            admin.setFirstName(jsonObject.getString("firstName"));
            admin.setLastName(jsonObject.getString("lastName"));
            admin.setEmail(jsonObject.getString("email"));
            admin.setActive(jsonObject.getBoolean("active"));

            if (!jsonObject.isNull("createdAt")) {
                admin.setCreatedAt(LocalDateTime.parse(jsonObject.getString("createdAt"),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            if (!jsonObject.isNull("lastLogin")) {
                admin.setLastLogin(LocalDateTime.parse(jsonObject.getString("lastLogin"),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            return admin;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing administrator payload", e);
            return null;
        }
    }

    private List<usuario_salud_dto> parseUsuariosSalud(String jsonString) {
        List<usuario_salud_dto> usuarios = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                JsonObject obj = value.asJsonObject();
                usuario_salud_dto dto = new usuario_salud_dto();

                if (obj.containsKey("id") && !obj.isNull("id")) {
                    dto.setId(obj.getJsonNumber("id").longValue());
                }
                if (obj.containsKey("cedula") && !obj.isNull("cedula")) {
                    dto.setCedula(obj.getString("cedula"));
                }
                if (obj.containsKey("nombreCompleto") && !obj.isNull("nombreCompleto")) {
                    dto.setNombreCompleto(obj.getString("nombreCompleto"));
                }
                if (obj.containsKey("primerNombre") && !obj.isNull("primerNombre")) {
                    dto.setPrimerNombre(obj.getString("primerNombre"));
                }
                if (obj.containsKey("primerApellido") && !obj.isNull("primerApellido")) {
                    dto.setPrimerApellido(obj.getString("primerApellido"));
                }
                if (obj.containsKey("email") && !obj.isNull("email")) {
                    dto.setEmail(obj.getString("email"));
                }
                if (obj.containsKey("active") && !obj.isNull("active")) {
                    dto.setActive(obj.getBoolean("active"));
                }
                if (obj.containsKey("createdAt") && !obj.isNull("createdAt")) {
                    String createdRaw = obj.getString("createdAt");
                    if (createdRaw != null && !createdRaw.isBlank()) {
                        dto.setCreatedAt(Instant.parse(createdRaw));
                    }
                }
                if (obj.containsKey("lastLogin") && !obj.isNull("lastLogin")) {
                    String lastLoginRaw = obj.getString("lastLogin");
                    if (lastLoginRaw != null && !lastLoginRaw.isBlank()) {
                        dto.setLastLogin(Instant.parse(lastLoginRaw));
                    }
                }

                usuarios.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing usuarios de salud payload", e);
            throw new RuntimeException("No se pudo interpretar la lista de usuarios de salud", e);
        }
        return usuarios;
    }

    private List<prestador_dto> parsePrestadores(String jsonString) {
        List<prestador_dto> prestadores = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                JsonObject obj = value.asJsonObject();
                prestador_dto dto = new prestador_dto();

                if (obj.containsKey("id") && !obj.isNull("id")) {
                    String idRaw = obj.getString("id");
                    if (idRaw != null && !idRaw.isBlank()) {
                        dto.setId(UUID.fromString(idRaw));
                    }
                }
                if (obj.containsKey("rut") && !obj.isNull("rut")) {
                    dto.setRut(obj.getString("rut"));
                }
                if (obj.containsKey("nombre") && !obj.isNull("nombre")) {
                    dto.setNombre(obj.getString("nombre"));
                }
                if (obj.containsKey("email") && !obj.isNull("email")) {
                    dto.setEmail(obj.getString("email"));
                }
                if (obj.containsKey("estado") && !obj.isNull("estado")) {
                    dto.setEstado(obj.getString("estado"));
                }
                if (obj.containsKey("tenantId") && !obj.isNull("tenantId")) {
                    String tenantRaw = obj.getString("tenantId");
                    if (tenantRaw != null && !tenantRaw.isBlank()) {
                        dto.setTenantId(UUID.fromString(tenantRaw));
                    }
                }

                prestadores.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing prestadores payload", e);
            throw new RuntimeException("No se pudo interpretar la lista de prestadores", e);
        }
        return prestadores;
    }

    private List<clinica_dto> parseClinicas(String jsonString) {
        List<clinica_dto> clinicas = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                JsonObject obj = value.asJsonObject();
                clinica_dto dto = new clinica_dto();

                if (obj.containsKey("tenantId") && !obj.isNull("tenantId")) {
                    String tenantRaw = obj.getString("tenantId");
                    if (tenantRaw != null && !tenantRaw.isBlank()) {
                        dto.setTenantId(UUID.fromString(tenantRaw));
                    }
                }
                if (obj.containsKey("nombre") && !obj.isNull("nombre")) {
                    dto.setNombre(obj.getString("nombre"));
                }
                if (obj.containsKey("direccion") && !obj.isNull("direccion")) {
                    dto.setDireccion(obj.getString("direccion"));
                }
                if (obj.containsKey("email") && !obj.isNull("email")) {
                    dto.setEmail(obj.getString("email"));
                }
                if (obj.containsKey("estado") && !obj.isNull("estado")) {
                    dto.setEstado(obj.getString("estado"));
                }
                if (obj.containsKey("fecRegistro") && !obj.isNull("fecRegistro")) {
                    String fecRegistroRaw = obj.getString("fecRegistro");
                    if (fecRegistroRaw != null && !fecRegistroRaw.isBlank()) {
                        dto.setFecRegistro(LocalDateTime.parse(fecRegistroRaw));
                    }
                }

                clinicas.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing clínicas payload", e);
            throw new RuntimeException("No se pudo interpretar la lista de clínicas", e);
        }
        return clinicas;
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonValue value = reader.read();
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject obj = value.asJsonObject();
                if (obj.containsKey("error") && !obj.isNull("error")) {
                    return obj.getString("error");
                }
                if (obj.containsKey("message") && !obj.isNull("message")) {
                    return obj.getString("message");
                }
            }
        } catch (Exception ignored) {
            // El cuerpo no es JSON o no tiene formato esperado
        }
        return body;
    }

    private CloseableHttpClient createHttpClient() {
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        return HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(sslSocketFactory)
                                .build())
                .build();
    }

    private String resolveBackendUrl() {
        String envValue = System.getenv(CENTRAL_ENV_VAR);
        if (envValue != null && !envValue.isBlank()) {
            return sanitizeBaseUrl(envValue);
        }
        String sysPropValue = System.getProperty(CENTRAL_SYS_PROP);
        if (sysPropValue != null && !sysPropValue.isBlank()) {
            return sanitizeBaseUrl(sysPropValue);
        }
        return sanitizeBaseUrl(DEFAULT_BACKEND_URL);
    }

    private String resolvePeripheralUrl() {
        String envValue = System.getenv(PERIPHERAL_ENV_VAR);
        if (envValue != null && !envValue.isBlank()) {
            return sanitizeBaseUrl(envValue);
        }
        String sysPropValue = System.getProperty(PERIPHERAL_SYS_PROP);
        if (sysPropValue != null && !sysPropValue.isBlank()) {
            return sanitizeBaseUrl(sysPropValue);
        }
        return sanitizeBaseUrl(DEFAULT_PERIPHERAL_URL);
    }

    private String sanitizeBaseUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String readEntityContent(CloseableHttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }
        try (InputStream stream = response.getEntity().getContent()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
