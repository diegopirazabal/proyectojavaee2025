package com.hcen.frontend.usuario.service;

import com.hcen.frontend.usuario.dto.administrador_clinica_dto;
import com.hcen.frontend.usuario.dto.profesional_salud_dto;
import com.hcen.frontend.usuario.dto.documento_historia_dto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

@ApplicationScoped
public class api_service {

    private static final String ENV_VAR_NAME = "HCEN_API_BASE_URL";
    private static final String SYS_PROP_NAME = "hcen.apiBaseUrl";
    // Por defecto apuntamos al backend periférico desplegado como hcen-periferico
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080/hcen-periferico/api";
    private static final String DEV_LOGIN_ENV = "HCEN_ENABLE_DEV_LOGIN";
    private static final String DEV_LOGIN_PROP = "hcen.enableDevLogin";
    private static final String DEV_USERNAME = "usuario";
    private static final String DEV_PASSWORD = "usuario";
    private static final String DEFAULT_CLINICA_ENV = "HCEN_DEFAULT_CLINICA_RUT";
    private static final String DEFAULT_CLINICA_PROP = "hcen.defaultClinicaRut";

    private final String backendUrl = resolveBackendUrl();
    // Notifications to central backend removed

    public administrador_clinica_dto authenticate(String username, String password) {
        if (isDevLoginEnabled() && DEV_USERNAME.equals(username) && DEV_PASSWORD.equals(password)) {
            administrador_clinica_dto dto = new administrador_clinica_dto();
            dto.setUsername(username);
            dto.setNombre("Dev");
            dto.setApellidos("Usuario");
            dto.setClinica(getDefaultClinicaRut());
            return dto;
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(backendUrl + "/auth/login");

            JsonObject loginData = Json.createObjectBuilder()
                .add("username", username)
                .add("password", password)
                .add("clinicaRut", getDefaultClinicaRut() == null ? "" : getDefaultClinicaRut())
                .build();

            request.setEntity(new StringEntity(loginData.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readAll(response.getEntity().getContent());
                    return parseAdminFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<profesional_salud_dto> getProfesionalesByEspecialidad(String especialidad) {
        List<profesional_salud_dto> result = new ArrayList<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String enc = URLEncoder.encode(especialidad, StandardCharsets.UTF_8);
            HttpGet request = new HttpGet(backendUrl + "/profesionales/especialidad/" + enc);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String json = readAll(response.getEntity().getContent());
                    try (JsonReader reader = Json.createReader(new StringReader(json))) {
                        JsonArray arr = reader.readArray();
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.getJsonObject(i);
                            profesional_salud_dto p = new profesional_salud_dto();
                            if (!o.isNull("ci")) p.setCi(o.getInt("ci"));
                            p.setNombre(o.getString("nombre", null));
                            p.setApellidos(o.getString("apellidos", null));
                            p.setEspecialidad(o.getString("especialidad", null));
                            p.setEmail(o.getString("email", null));
                            result.add(p);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<documento_historia_dto> listHistoria(String usuario) {
        List<documento_historia_dto> result = new ArrayList<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = backendUrl + "/historia/" + URLEncoder.encode(usuario, StandardCharsets.UTF_8) + "/documentos";
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String json = readAll(response.getEntity().getContent());
                    try (JsonReader reader = Json.createReader(new StringReader(json))) {
                        JsonArray arr = reader.readArray();
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.getJsonObject(i);
                            documento_historia_dto d = new documento_historia_dto();
                            d.setId(o.getString("id", null));
                            d.setTitle(o.getString("title", null));
                            if (o.containsKey("createdAt") && !o.isNull("createdAt")) {
                                d.setCreatedAt(java.time.Instant.parse(o.getString("createdAt")));
                            } else if (o.containsKey("created") && !o.isNull("created")) {
                                d.setCreatedAt(java.time.Instant.parse(o.getString("created")));
                            }
                            if (o.containsKey("formats") && o.get("formats").getValueType() == jakarta.json.JsonValue.ValueType.ARRAY) {
                                JsonArray f = o.getJsonArray("formats");
                                for (int j = 0; j < f.size(); j++) d.getFormats().add(f.getString(j));
                            }
                            result.add(d);
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return result;
    }

    public String fetchHistoriaHtml(String usuario, String docId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = backendUrl + "/historia/" + URLEncoder.encode(usuario, StandardCharsets.UTF_8)
                    + "/documentos/" + URLEncoder.encode(docId, StandardCharsets.UTF_8) + "?format=html";
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "text/html");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200 && response.getEntity() != null) {
                    return readAll(response.getEntity().getContent());
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    public String fetchHistoriaPdfBase64(String usuario, String docId, boolean preferirPdf) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String base = backendUrl + "/historia/" + URLEncoder.encode(usuario, StandardCharsets.UTF_8)
                    + "/documentos/" + (docId == null ? URLEncoder.encode(usuario, StandardCharsets.UTF_8) : URLEncoder.encode(docId, StandardCharsets.UTF_8));
            String url = base + (docId == null ? "" : "") + "?format=pdf";
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/pdf");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200 && response.getEntity() != null) {
                    byte[] bytes = response.getEntity().getContent().readAllBytes();
                    return Base64.getEncoder().encodeToString(bytes);
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    public String fetchHistoriaClinicaPdfBase64(String usuario, String clinicaRut) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = backendUrl + "/historia/" + URLEncoder.encode(usuario, StandardCharsets.UTF_8);
            if (clinicaRut != null && !clinicaRut.isBlank()) {
                url += "?clinicaRut=" + URLEncoder.encode(clinicaRut, StandardCharsets.UTF_8);
            }
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/pdf");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200 && response.getEntity() != null) {
                    byte[] bytes = response.getEntity().getContent().readAllBytes();
                    return Base64.getEncoder().encodeToString(bytes);
                }
            }
        } catch (Exception e) {
            // ignore and return null
        }
        return null;
    }

    // Notifications removed

    private administrador_clinica_dto parseAdminFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject json = reader.readObject();
            administrador_clinica_dto dto = new administrador_clinica_dto();
            if (!json.isNull("id")) {
                try {
                    dto.setId(java.util.UUID.fromString(json.getString("id"))); // backend returns UUID
                } catch (Exception ignore) { dto.setId(null); }
            }
            dto.setUsername(json.getString("username", null));
            dto.setNombre(json.getString("nombre", null));
            dto.setApellidos(json.getString("apellidos", null));
            dto.setClinica(json.getString("clinica", null));
            return dto;
        }
    }

    private String resolveBackendUrl() {
        String envValue = System.getenv(ENV_VAR_NAME);
        if (envValue != null && !envValue.isBlank()) {
            return sanitizeBaseUrl(envValue);
        }
        String sysPropValue = System.getProperty(SYS_PROP_NAME);
        if (sysPropValue != null && !sysPropValue.isBlank()) {
            return sanitizeBaseUrl(sysPropValue);
        }
        return sanitizeBaseUrl(DEFAULT_BACKEND_URL);
    }

    private String sanitizeBaseUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    // resolveCentralBackendUrl removed

    private String readAll(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private boolean isDevLoginEnabled() {
        String envValue = System.getenv(DEV_LOGIN_ENV);
        if (envValue != null) {
            return Boolean.parseBoolean(envValue);
        }
        return Boolean.parseBoolean(System.getProperty(DEV_LOGIN_PROP, "false"));
    }

    public String getDefaultClinicaRut() {
        String envValue = System.getenv(DEFAULT_CLINICA_ENV);
        if (envValue != null && !envValue.isBlank()) return envValue.trim();
        String prop = System.getProperty(DEFAULT_CLINICA_PROP);
        if (prop != null && !prop.isBlank()) return prop.trim();
        return null;
    }
}
