package com.hcen.periferico.frontend.service;

import com.hcen.periferico.frontend.dto.administrador_clinica_dto;
import com.hcen.periferico.frontend.dto.clinica_dto;
import com.hcen.periferico.frontend.dto.configuracion_clinica_dto;
import com.hcen.periferico.frontend.dto.especialidad_dto;
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
    private static final String BACKEND_URL_PROD = "https://node5823-hcen-uy.web.elasticloud.uy/multitenant-api";
    private static final String BACKEND_URL_DEV = "http://localhost:8080/multitenant-api";

    private String getBackendUrl() {
        // Usar variable de entorno, o detectar por hostname, o System property
        String env = System.getProperty("app.environment", "development");
        return "production".equalsIgnoreCase(env) ? BACKEND_URL_PROD : BACKEND_URL_DEV;
    }

    private String BACKEND_URL() {
        return getBackendUrl();
    }

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
            HttpPost request = new HttpPost(BACKEND_URL() + "/auth/login");

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
            HttpGet request = new HttpGet(BACKEND_URL() + "/clinicas");

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
            HttpGet request = new HttpGet(BACKEND_URL() + "/configuracion/" + clinicaRut);

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
            HttpPut request = new HttpPut(BACKEND_URL() + "/configuracion/" + clinicaRut + "/lookfeel");

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
            HttpPut request = new HttpPut(BACKEND_URL() + "/configuracion/" + clinicaRut + "/nodo");

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
            HttpPost request = new HttpPost(BACKEND_URL() + "/configuracion/" + clinicaRut + "/reset");

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

    public List<profesional_salud_dto> getAllProfesionales(String tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/profesionales?tenantId=" + tenantId);

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

    public List<profesional_salud_dto> searchProfesionales(String searchTerm, String tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String encodedTerm = java.net.URLEncoder.encode(searchTerm, java.nio.charset.StandardCharsets.UTF_8);
            HttpGet request = new HttpGet(BACKEND_URL() + "/profesionales?tenantId=" + tenantId + "&search=" + encodedTerm);

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
                                               String especialidadId, String email, String password, String tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(BACKEND_URL() + "/profesionales?tenantId=" + tenantId);

            JsonObject data = Json.createObjectBuilder()
                .add("ci", ci)
                .add("nombre", nombre)
                .add("apellidos", apellidos)
                .add("especialidadId", especialidadId != null ? especialidadId : "")
                .add("email", email != null ? email : "")
                .add("password", password != null ? password : "")
                .build();

            StringEntity entity = new StringEntity(data.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());

                if (response.getCode() == 200) {
                    return parseProfesionalFromJson(responseBody);
                } else if (response.getCode() == 400) {
                    // Error de validación - parsear mensaje de error
                    String errorMessage = parseErrorMessage(responseBody);
                    throw new IllegalArgumentException(errorMessage);
                } else {
                    // Otro error
                    String errorMessage = parseErrorMessage(responseBody);
                    throw new RuntimeException("Error del servidor: " + errorMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error de comunicación con el servidor");
        }
    }

    public boolean deleteProfesional(Integer ci, String tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpDelete request = new HttpDelete(BACKEND_URL() + "/profesionales/" + ci + "?tenantId=" + tenantId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Obtiene todas las especialidades médicas disponibles
     */
    public List<especialidad_dto> getEspecialidades() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/especialidades");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseEspecialidadesFromJson(responseBody);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
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
            dto.setEspecialidadId(jsonObject.getString("especialidadId", null));
            dto.setEmail(jsonObject.getString("email", null));
            dto.setTenantId(jsonObject.getString("tenantId", null));

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
                dto.setEspecialidadId(jsonObject.getString("especialidadId", null));
                dto.setEmail(jsonObject.getString("email", null));
                dto.setTenantId(jsonObject.getString("tenantId", null));

                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<especialidad_dto> parseEspecialidadesFromJson(String jsonString) {
        List<especialidad_dto> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray jsonArray = reader.readArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);

                especialidad_dto dto = new especialidad_dto();
                dto.setId(jsonObject.getString("id"));
                dto.setNombre(jsonObject.getString("nombre"));

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
            HttpGet request = new HttpGet(BACKEND_URL() + "/usuarios?tenantId=" + tenantId);

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
            HttpGet request = new HttpGet(BACKEND_URL() + "/usuarios?tenantId=" + tenantId + "&search=" + encodedTerm);

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
                                             String tenantId) throws RuntimeException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BACKEND_URL() + "/usuarios/registrar");

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
                String responseBody = new String(response.getEntity().getContent().readAllBytes());

                if (response.getCode() == 200) {
                    return parseUsuarioFromJson(responseBody);
                } else {
                    // Extraer mensaje de error del backend
                    String errorMessage = extractErrorMessage(responseBody);
                    throw new RuntimeException(errorMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error de comunicación con el servidor");
        }
    }

    public boolean deleteUsuario(String cedula, String tenantId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BACKEND_URL() + "/usuarios/" + cedula + "/clinica/" + tenantId);

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
            dto.setTenantId(jsonObject.getString("tenantId", null));

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
            JsonArray jsonArray = reader.readArray();

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
                dto.setTenantId(jsonObject.getString("tenantId", null));

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

    // ========== DOCUMENTOS CLÍNICOS ==========

    public com.hcen.periferico.frontend.dto.documento_clinico_dto crearDocumento(
            String usuarioSaludCedula, Integer profesionalCi, String codigoMotivoConsulta,
            String descripcionDiagnostico, String fechaInicioDiagnostico, String codigoEstadoProblema,
            String codigoGradoCerteza, String fechaProximaConsulta, String descripcionProximaConsulta,
            String referenciaAlta, UUID tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(BACKEND_URL() + "/documentos?tenantId=" + tenantId);

            var builder = Json.createObjectBuilder()
                    .add("usuarioSaludCedula", usuarioSaludCedula)
                    .add("profesionalCi", profesionalCi)
                    .add("codigoMotivoConsulta", codigoMotivoConsulta)
                    .add("descripcionDiagnostico", descripcionDiagnostico)
                    .add("fechaInicioDiagnostico", fechaInicioDiagnostico)
                    .add("codigoGradoCerteza", codigoGradoCerteza);

            if (codigoEstadoProblema != null && !codigoEstadoProblema.isEmpty()) {
                builder.add("codigoEstadoProblema", codigoEstadoProblema);
            }
            if (fechaProximaConsulta != null && !fechaProximaConsulta.isEmpty()) {
                builder.add("fechaProximaConsulta", fechaProximaConsulta);
            }
            if (descripcionProximaConsulta != null && !descripcionProximaConsulta.isEmpty()) {
                builder.add("descripcionProximaConsulta", descripcionProximaConsulta);
            }
            if (referenciaAlta != null && !referenciaAlta.isEmpty()) {
                builder.add("referenciaAlta", referenciaAlta);
            }

            StringEntity entity = new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                if (response.getCode() == 201 || response.getCode() == 200) {
                    return parseDocumentoFromJson(responseBody);
                } else {
                    System.err.println("Error al crear documento: " + extractErrorMessage(responseBody));
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<com.hcen.periferico.frontend.dto.documento_clinico_dto> getDocumentosPorPaciente(
            String cedula, UUID tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/documentos/paciente/" + cedula + "?tenantId=" + tenantId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    // La respuesta es un objeto con {data: [...], totalCount: n}
                    return parseDocumentosListFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public com.hcen.periferico.frontend.dto.documento_clinico_dto getDocumentoPorId(
            String documentoId, UUID tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/documentos/" + documentoId + "?tenantId=" + tenantId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseDocumentoFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.util.Map<String, String> getMotivosConsulta() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/documentos/catalogos/motivos");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.HashMap<>();
    }

    public java.util.Map<String, String> getEstadosProblema() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/documentos/catalogos/estados");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.HashMap<>();
    }

    public java.util.Map<String, String> getGradosCerteza() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/documentos/catalogos/grados-certeza");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.HashMap<>();
    }

    private com.hcen.periferico.frontend.dto.documento_clinico_dto parseDocumentoFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject json = reader.readObject();
            com.hcen.periferico.frontend.dto.documento_clinico_dto dto =
                new com.hcen.periferico.frontend.dto.documento_clinico_dto();

            dto.setId(json.getString("id", null));
            dto.setTenantId(json.getString("tenantId", null));
            dto.setUsuarioSaludCedula(json.getString("usuarioSaludCedula", null));
            dto.setProfesionalCi(json.getInt("profesionalCi", 0));
            dto.setNombreCompletoPaciente(json.getString("nombreCompletoPaciente", null));
            dto.setNombreCompletoProfesional(json.getString("nombreCompletoProfesional", null));
            dto.setEspecialidadProfesional(json.getString("especialidadProfesional", null));
            dto.setCodigoMotivoConsulta(json.getString("codigoMotivoConsulta", null));
            dto.setNombreMotivoConsulta(json.getString("nombreMotivoConsulta", null));
            dto.setDescripcionDiagnostico(json.getString("descripcionDiagnostico", null));

            String fechaInicio = json.getString("fechaInicioDiagnostico", null);
            if (fechaInicio != null) {
                dto.setFechaInicioDiagnostico(java.time.LocalDate.parse(fechaInicio));
            }

            dto.setCodigoEstadoProblema(json.getString("codigoEstadoProblema", null));
            dto.setNombreEstadoProblema(json.getString("nombreEstadoProblema", null));
            dto.setCodigoGradoCerteza(json.getString("codigoGradoCerteza", null));
            dto.setNombreGradoCerteza(json.getString("nombreGradoCerteza", null));

            String fechaProxima = json.getString("fechaProximaConsulta", null);
            if (fechaProxima != null) {
                dto.setFechaProximaConsulta(java.time.LocalDate.parse(fechaProxima));
            }

            dto.setDescripcionProximaConsulta(json.getString("descripcionProximaConsulta", null));
            dto.setReferenciaAlta(json.getString("referenciaAlta", null));

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<com.hcen.periferico.frontend.dto.documento_clinico_dto> parseDocumentosListFromJson(String jsonString) {
        List<com.hcen.periferico.frontend.dto.documento_clinico_dto> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject response = reader.readObject();
            JsonArray jsonArray = response.getJsonArray("data");

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject json = jsonArray.getJsonObject(i);
                com.hcen.periferico.frontend.dto.documento_clinico_dto dto = parseDocumentoFromJsonObject(json);
                if (dto != null) {
                    list.add(dto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private com.hcen.periferico.frontend.dto.documento_clinico_dto parseDocumentoFromJsonObject(JsonObject json) {
        try {
            com.hcen.periferico.frontend.dto.documento_clinico_dto dto =
                new com.hcen.periferico.frontend.dto.documento_clinico_dto();

            dto.setId(json.getString("id", null));
            dto.setTenantId(json.getString("tenantId", null));
            dto.setUsuarioSaludCedula(json.getString("usuarioSaludCedula", null));
            dto.setProfesionalCi(json.getInt("profesionalCi", 0));
            dto.setNombreCompletoPaciente(json.getString("nombreCompletoPaciente", null));
            dto.setNombreCompletoProfesional(json.getString("nombreCompletoProfesional", null));
            dto.setEspecialidadProfesional(json.getString("especialidadProfesional", null));
            dto.setCodigoMotivoConsulta(json.getString("codigoMotivoConsulta", null));
            dto.setNombreMotivoConsulta(json.getString("nombreMotivoConsulta", null));
            dto.setDescripcionDiagnostico(json.getString("descripcionDiagnostico", null));

            String fechaInicio = json.getString("fechaInicioDiagnostico", null);
            if (fechaInicio != null) {
                dto.setFechaInicioDiagnostico(java.time.LocalDate.parse(fechaInicio));
            }

            dto.setCodigoEstadoProblema(json.getString("codigoEstadoProblema", null));
            dto.setNombreEstadoProblema(json.getString("nombreEstadoProblema", null));
            dto.setCodigoGradoCerteza(json.getString("codigoGradoCerteza", null));
            dto.setNombreGradoCerteza(json.getString("nombreGradoCerteza", null));

            String fechaProxima = json.getString("fechaProximaConsulta", null);
            if (fechaProxima != null) {
                dto.setFechaProximaConsulta(java.time.LocalDate.parse(fechaProxima));
            }

            dto.setDescripcionProximaConsulta(json.getString("descripcionProximaConsulta", null));
            dto.setReferenciaAlta(json.getString("referenciaAlta", null));

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private java.util.Map<String, String> parseMapFromJson(String jsonString) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject json = reader.readObject();
            for (String key : json.keySet()) {
                map.put(key, json.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public boolean isBackendAvailable() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(BACKEND_URL() + "/auth/login");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() != 500;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrae el mensaje de error del JSON de respuesta del backend
     * Formato esperado: {"error": "mensaje de error"}
     */
    private String extractErrorMessage(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();
            String errorMessage = null;

            if (jsonObject.containsKey("error")) {
                errorMessage = jsonObject.getString("error");
            } else if (jsonObject.containsKey("message")) {
                errorMessage = jsonObject.getString("message");
            } else {
                errorMessage = "Error desconocido del servidor";
            }

            // Limpiar prefijos de excepciones si existen
            return cleanExceptionPrefix(errorMessage);
        } catch (Exception e) {
            // Si no se puede parsear el JSON, retornar el string completo (truncado)
            String message = jsonString != null ? jsonString : "Error desconocido del servidor";
            if (message.length() > 200) {
                message = message.substring(0, 200) + "...";
            }
            return cleanExceptionPrefix(message);
        }
    }

    /**
     * Parsea el mensaje de error del JSON ErrorResponse
     * Ejemplo JSON: {"message": "La contraseña debe tener..."}
     */
    private String parseErrorMessage(String jsonResponse) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonResponse))) {
            JsonObject jsonObject = reader.readObject();
            if (jsonObject.containsKey("message")) {
                return jsonObject.getString("message");
            } else if (jsonObject.containsKey("error")) {
                return jsonObject.getString("error");
            }
            return "Error desconocido";
        } catch (Exception e) {
            // Si no se puede parsear el JSON, retornar el texto plano
            return jsonResponse;
        }
    }

    /**
     * Elimina prefijos de excepciones Java del mensaje
     * Ejemplo: "java.lang.IllegalArgumentException: mensaje" -> "mensaje"
     */
    private String cleanExceptionPrefix(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Si el mensaje contiene ": ", tomar solo lo que viene después del último ":"
        int lastColonIndex = message.lastIndexOf(": ");
        if (lastColonIndex != -1 && lastColonIndex < message.length() - 2) {
            return message.substring(lastColonIndex + 2).trim();
        }

        return message;
    }
}
