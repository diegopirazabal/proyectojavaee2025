package com.hcen.periferico.frontend.service;

import com.hcen.periferico.frontend.dto.administrador_clinica_dto;
import com.hcen.periferico.frontend.dto.configuracion_clinica_dto;
import com.hcen.periferico.frontend.dto.profesional_salud_dto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class APIService implements Serializable {

    private static final long serialVersionUID = 1L;
    // URL base configurable del backend periférico
    private static final String DEFAULT_BASE = "http://localhost:8080/hcen-periferico/multitenant-api";
    private static final String BACKEND_URL_FALLBACK = resolveBaseUrl();

    @Inject
    private transient ServletContext servletContext;

    // Si está definido en web.xml (context-param), tendrá prioridad
    private String baseFromWebXml;

    @PostConstruct
    public void init() {
        try {
            if (servletContext != null) {
                String p = servletContext.getInitParameter("hcen.periferico.api.base");
                if (p != null && !p.isBlank()) {
                    baseFromWebXml = p.trim();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String resolveBaseUrl() {
        try {
            String fromEnv = System.getenv("HCEN_PERIFERICO_API_BASE");
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv.trim();
            }
            String fromProp = System.getProperty("hcen.periferico.api.base");
            if (fromProp != null && !fromProp.isBlank()) {
                return fromProp.trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return DEFAULT_BASE;
    }

    private String effectiveBase() {
        if (baseFromWebXml != null && !baseFromWebXml.isBlank()) {
            return baseFromWebXml;
        }
        return BACKEND_URL_FALLBACK;
    }

    // ========== AUTENTICACIÓN ==========

    public administrador_clinica_dto authenticate(String username, String password, String clinicaRut) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(effectiveBase() + "/auth/login");

            JsonObject loginData = Json.createObjectBuilder()
                .add("username", username)
                .add("password", password)
                .add("clinicaRut", clinicaRut)
                .build();

            StringEntity entity = new StringEntity(loginData.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseAdministradorFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ========== CONFIGURACIÓN ==========

    public configuracion_clinica_dto getConfiguracion(String clinicaRut) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(effectiveBase() + "/configuracion/" + clinicaRut);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseConfiguracionFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public configuracion_clinica_dto updateLookAndFeel(String clinicaRut, String colorPrimario,
                                                     String colorSecundario, String logoUrl,
                                                     String nombreSistema, String tema) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(effectiveBase() + "/configuracion/" + clinicaRut + "/lookfeel");

            JsonObject data = Json.createObjectBuilder()
                .add("colorPrimario", colorPrimario != null ? colorPrimario : "")
                .add("colorSecundario", colorSecundario != null ? colorSecundario : "")
                .add("logoUrl", logoUrl != null ? logoUrl : "")
                .add("nombreSistema", nombreSistema != null ? nombreSistema : "")
                .add("tema", tema != null ? tema : "")
                .build();

            StringEntity entity = new StringEntity(data.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseConfiguracionFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public configuracion_clinica_dto toggleNodoPeriferico(String clinicaRut, boolean habilitado) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(effectiveBase() + "/configuracion/" + clinicaRut + "/nodo");

            JsonObject data = Json.createObjectBuilder()
                .add("habilitado", habilitado)
                .build();

            StringEntity entity = new StringEntity(data.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseConfiguracionFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public configuracion_clinica_dto resetConfiguracion(String clinicaRut) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(effectiveBase() + "/configuracion/" + clinicaRut + "/reset");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseConfiguracionFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ========== PROFESIONALES ==========

    public List<profesional_salud_dto> getAllProfesionales() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(effectiveBase() + "/profesionales");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseProfesionalesFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<profesional_salud_dto> searchProfesionales(String searchTerm) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String encodedTerm = java.net.URLEncoder.encode(searchTerm, java.nio.charset.StandardCharsets.UTF_8);
            HttpGet request = new HttpGet(effectiveBase() + "/profesionales?search=" + encodedTerm);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseProfesionalesFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public profesional_salud_dto saveProfesional(Integer ci, String nombre, String apellidos,
                                               String especialidad, String email) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(effectiveBase() + "/profesionales");

            JsonObject data = Json.createObjectBuilder()
                .add("ci", ci)
                .add("nombre", nombre)
                .add("apellidos", apellidos)
                .add("especialidad", especialidad != null ? especialidad : "")
                .add("email", email != null ? email : "")
                .build();

            StringEntity entity = new StringEntity(data.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseProfesionalFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteProfesional(Integer ci) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(effectiveBase() + "/profesionales/" + ci);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ========== MÉTODOS DE PARSEO JSON ==========

    private administrador_clinica_dto parseAdministradorFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            administrador_clinica_dto dto = new administrador_clinica_dto();
            dto.setId(UUID.fromString(jsonObject.getString("id")));
            dto.setUsername(jsonObject.getString("username"));
            dto.setNombre(jsonObject.getString("nombre"));
            dto.setApellidos(jsonObject.getString("apellidos"));
            dto.setClinica(jsonObject.getString("clinica"));

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private configuracion_clinica_dto parseConfiguracionFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            configuracion_clinica_dto dto = new configuracion_clinica_dto();
            if (!jsonObject.isNull("id")) {
                dto.setId(UUID.fromString(jsonObject.getString("id")));
            }
            dto.setClinicaRut(jsonObject.getString("clinicaRut", null));
            dto.setColorPrimario(jsonObject.getString("colorPrimario", null));
            dto.setColorSecundario(jsonObject.getString("colorSecundario", null));
            dto.setLogoUrl(jsonObject.getString("logoUrl", null));
            dto.setNombreSistema(jsonObject.getString("nombreSistema", null));
            dto.setTema(jsonObject.getString("tema", null));
            dto.setNodoPerifericoHabilitado(jsonObject.getBoolean("nodoPerifericoHabilitado", false));

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private profesional_salud_dto parseProfesionalFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            profesional_salud_dto dto = new profesional_salud_dto();
            dto.setCi(jsonObject.getInt("ci"));
            dto.setNombre(jsonObject.getString("nombre"));
            dto.setApellidos(jsonObject.getString("apellidos"));
            dto.setEspecialidad(jsonObject.getString("especialidad", null));
            dto.setEmail(jsonObject.getString("email", null));

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<profesional_salud_dto> parseProfesionalesFromJson(String jsonString) {
        List<profesional_salud_dto> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject paginatedResponse = reader.readObject();
            JsonArray jsonArray = paginatedResponse.getJsonArray("data");

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);

                profesional_salud_dto dto = new profesional_salud_dto();
                dto.setCi(jsonObject.getInt("ci"));
                dto.setNombre(jsonObject.getString("nombre"));
                dto.setApellidos(jsonObject.getString("apellidos"));
                dto.setEspecialidad(jsonObject.getString("especialidad", null));
                dto.setEmail(jsonObject.getString("email", null));

                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean isBackendAvailable() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(effectiveBase() + "/auth/login");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() != 500;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
