package hcen.frontend.admin.service;

import hcen.frontend.admin.dto.AdminHCENDTO;
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

@ApplicationScoped
public class APIService {
    
    private static final String BACKEND_URL = "http://localhost:8080/hcen-central/api";
    
    public AdminHCENDTO authenticate(String username, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BACKEND_URL + "/auth/login");
            
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
    
    private AdminHCENDTO parseAdminFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();
            
            AdminHCENDTO admin = new AdminHCENDTO();
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
            HttpPost request = new HttpPost(BACKEND_URL + "/health");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (Exception e) {
            return false;
        }
    }
}