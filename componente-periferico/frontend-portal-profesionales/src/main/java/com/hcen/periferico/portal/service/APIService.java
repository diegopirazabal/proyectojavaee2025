package com.hcen.periferico.portal.service;

import com.hcen.periferico.portal.dto.ClinicaDTO;
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
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class APIService {

    private static final String BACKEND_URL = "http://localhost:8080/multitenant-api";

    private CloseableHttpClient createHttpClient() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
            );

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .build();

            return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo inicializar SSL inseguro", e);
        }
    }

    public JsonObject loginProfesional(String tenantId, String username, String password) throws IOException {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpPost post = new HttpPost(BACKEND_URL + "/auth/profesional/login");
            JsonObject payload = Json.createObjectBuilder()
                .add("tenantId", tenantId)
                .add("email", username)
                .add("password", password)
                .build();
            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getCode();
                String body = response.getEntity() != null ? new String(response.getEntity().getContent().readAllBytes()) : "{}";
                if (status >= 200 && status < 300) {
                    try (var reader = jakarta.json.Json.createReader(new java.io.StringReader(body))) {
                        return reader.readObject();
                    }
                }
                throw new IOException("Error de login: HTTP " + status + " - " + body);
            }
        }
    }

    public List<ClinicaDTO> getClinicas() throws IOException {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet get = new HttpGet(BACKEND_URL + "/clinicas");
            try (CloseableHttpResponse response = client.execute(get)) {
                int status = response.getCode();
                String body = response.getEntity() != null ? new String(response.getEntity().getContent().readAllBytes()) : "[]";
                if (status >= 200 && status < 300) {
                    return parseClinicasFromJson(body);
                }
                throw new IOException("Error al obtener clínicas: HTTP " + status + " - " + body);
            }
        }
    }

    private List<ClinicaDTO> parseClinicasFromJson(String jsonString) {
        List<ClinicaDTO> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray jsonArray = reader.readArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);
                String tenantId = jsonObject.getString("tenantId", null);
                String nombre = jsonObject.getString("nombre", "");
                if (tenantId != null) {
                    list.add(new ClinicaDTO(tenantId, nombre));
                }
            }
        } catch (Exception e) {
            // devuelve lista vacía si hay error de parseo
        }
        return list;
    }

    public boolean isBackendAvailable() {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet get = new HttpGet(BACKEND_URL + "/clinicas");
            try (CloseableHttpResponse response = client.execute(get)) {
                return response.getCode() != 500;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
