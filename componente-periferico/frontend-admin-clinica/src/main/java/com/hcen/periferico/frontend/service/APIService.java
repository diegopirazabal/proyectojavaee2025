package com.hcen.periferico.frontend.service;

import com.hcen.periferico.frontend.dto.administrador_clinica_dto;
import com.hcen.periferico.frontend.dto.clinica_dto;
import com.hcen.periferico.frontend.dto.configuracion_clinica_dto;
import com.hcen.periferico.frontend.dto.profesional_salud_dto;
import com.hcen.periferico.frontend.dto.usuario_salud_dto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.hc.client5.http.classic.methods.*;
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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.SSLContext;

@ApplicationScoped
public class APIService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String BACKEND_URL = "https://node5823-hcen-uy.web.elasticloud.uy/multitenant-api";

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
            throw new IllegalStateException("Unable to initialize insecure SSL context", e);
        }
    }

    // ========== AUTENTICACIÓN ==========

    public administrador_clinica_dto authenticate(String username, String password, String tenantId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BACKEND_URL + "/auth/login");

            JsonObject loginData = Json.createObjectBuilder()
                .add("username", username)
                .add("password", password)
                .add("tenantId", tenantId)
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

    public List<clinica_dto> getClinicas() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BACKEND_URL + "/clinicas");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseClinicasFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ========== CONFIGURACIÓN ==========

    public configuracion_clinica_dto getConfiguracion(String clinicaRut) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL + "/configuracion/" + clinicaRut);

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(BACKEND_URL + "/configuracion/" + clinicaRut + "/lookfeel");

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(BACKEND_URL + "/configuracion/" + clinicaRut + "/nodo");

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(BACKEND_URL + "/configuracion/" + clinicaRut + "/reset");

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL + "/profesionales");

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String encodedTerm = java.net.URLEncoder.encode(searchTerm, java.nio.charset.StandardCharsets.UTF_8);
            HttpGet request = new HttpGet(BACKEND_URL + "/profesionales?search=" + encodedTerm);

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(BACKEND_URL + "/profesionales");

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
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpDelete request = new HttpDelete(BACKEND_URL + "/profesionales/" + ci);

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
            dto.setTenantId(UUID.fromString(jsonObject.getString("tenantId")));

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<clinica_dto> parseClinicasFromJson(String jsonString) {
        List<clinica_dto> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray jsonArray = reader.readArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);

                clinica_dto dto = new clinica_dto();
                dto.setTenantId(jsonObject.getString("tenantId"));
                dto.setNombre(jsonObject.getString("nombre"));

                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
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

    // ========== USUARIOS DE SALUD ==========

    public List<usuario_salud_dto> getAllUsuarios(String tenantId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BACKEND_URL + "/usuarios?tenantId=" + tenantId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseUsuariosFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<usuario_salud_dto> searchUsuarios(String searchTerm, String tenantId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String encodedTerm = java.net.URLEncoder.encode(searchTerm, java.nio.charset.StandardCharsets.UTF_8);
            HttpGet request = new HttpGet(BACKEND_URL + "/usuarios?tenantId=" + tenantId + "&search=" + encodedTerm);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseUsuariosFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public usuario_salud_dto registrarUsuario(String cedula, String tipoDocumento,
                                             String primerNombre, String segundoNombre,
                                             String primerApellido, String segundoApellido,
                                             String email, java.time.LocalDate fechaNacimiento,
                                             String tenantId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BACKEND_URL + "/usuarios/registrar");

            var builder = Json.createObjectBuilder()
                .add("cedula", cedula)
                .add("tipoDocumento", tipoDocumento)
                .add("primerNombre", primerNombre)
                .add("primerApellido", primerApellido)
                .add("email", email)
                .add("fechaNacimiento", fechaNacimiento.toString())
                .add("tenantId", tenantId);

            if (segundoNombre != null && !segundoNombre.isEmpty()) {
                builder.add("segundoNombre", segundoNombre);
            }
            if (segundoApellido != null && !segundoApellido.isEmpty()) {
                builder.add("segundoApellido", segundoApellido);
            }

            JsonObject data = builder.build();

            StringEntity entity = new StringEntity(data.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseUsuarioFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteUsuario(String cedula, String tenantId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BACKEND_URL + "/usuarios/" + cedula + "/clinica/" + tenantId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private usuario_salud_dto parseUsuarioFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            usuario_salud_dto dto = new usuario_salud_dto();
            dto.setCedula(jsonObject.getString("cedula", null));
            dto.setTipoDocumento(jsonObject.getString("tipoDocumento", null));
            dto.setPrimerNombre(jsonObject.getString("primerNombre", null));
            dto.setSegundoNombre(jsonObject.getString("segundoNombre", null));
            dto.setPrimerApellido(jsonObject.getString("primerApellido", null));
            dto.setSegundoApellido(jsonObject.getString("segundoApellido", null));
            dto.setEmail(jsonObject.getString("email", null));

            String fechaNacStr = jsonObject.getString("fechaNacimiento", null);
            if (fechaNacStr != null) {
                dto.setFechaNacimiento(java.time.LocalDate.parse(fechaNacStr));
            }

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<usuario_salud_dto> parseUsuariosFromJson(String jsonString) {
        List<usuario_salud_dto> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject paginatedResponse = reader.readObject();
            JsonArray jsonArray = paginatedResponse.getJsonArray("data");

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);

                usuario_salud_dto dto = new usuario_salud_dto();
                dto.setCedula(jsonObject.getString("cedula", null));
                dto.setTipoDocumento(jsonObject.getString("tipoDocumento", null));
                dto.setPrimerNombre(jsonObject.getString("primerNombre", null));
                dto.setSegundoNombre(jsonObject.getString("segundoNombre", null));
                dto.setPrimerApellido(jsonObject.getString("primerApellido", null));
                dto.setSegundoApellido(jsonObject.getString("segundoApellido", null));
                dto.setEmail(jsonObject.getString("email", null));

                String fechaNacStr = jsonObject.getString("fechaNacimiento", null);
                if (fechaNacStr != null) {
                    dto.setFechaNacimiento(java.time.LocalDate.parse(fechaNacStr));
                }

                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean isBackendAvailable() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL + "/auth/login");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() != 500;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
