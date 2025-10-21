package hcen.frontend.admin.service;

import hcen.frontend.admin.dto.admin_hcen_dto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

@ApplicationScoped
public class api_service {

    private static final String ENV_VAR_NAME = "HCEN_API_BASE_URL";
    private static final String SYS_PROP_NAME = "hcen.apiBaseUrl";
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080/hcen-central/api";
    private static final Logger LOGGER = Logger.getLogger(api_service.class.getName());
    private final String backendUrl = resolveBackendUrl();
    
    public admin_hcen_dto authenticate(String username, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(backendUrl + "/auth/login");
            
            JsonObject loginData = Json.createObjectBuilder()
                .add("username", username)
                .add("password", password)
                .build();
            
            StringEntity entity = new StringEntity(loginData.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseAdminFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean triggerBroadcastNotification() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(backendUrl + "/notifications/broadcast-test");

            StringEntity entity = new StringEntity("{}", ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean isBackendAvailable() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(backendUrl + "/health");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (Exception e) {
            return false;
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

}
