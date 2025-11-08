package com.hcen.periferico.profesional.service;

import com.hcen.periferico.profesional.dto.clinica_dto;
import com.hcen.periferico.profesional.dto.documento_clinico_dto;
import com.hcen.periferico.profesional.dto.profesional_salud_dto;
import com.hcen.periferico.profesional.dto.usuario_salud_dto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLContext;

@ApplicationScoped
public class APIService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String BACKEND_URL_PROD = "https://node5823-hcen-uy.web.elasticloud.uy/multitenant-api";
    private static final String BACKEND_URL_DEV = "http://localhost:8080/multitenant-api";

    private String getBackendUrl() {
        String env = System.getProperty("app.environment", "development");
        return "production".equalsIgnoreCase(env) ? BACKEND_URL_PROD : BACKEND_URL_DEV;
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

    // ========== AUTENTICACIÓN Y CATÁLOGOS ==========

    public profesional_salud_dto loginProfesional(String tenantId, String email, String password) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(getBackendUrl() + "/auth/profesional/login");

            var json = Json.createObjectBuilder()
                .add("tenantId", tenantId)
                .add("email", email)
                .add("password", password)
                .build();

            request.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                String responseBody = response.getEntity() != null
                    ? new String(response.getEntity().getContent().readAllBytes())
                    : "";

                if (status == 200) {
                    return parseProfesionalFromJson(responseBody);
                }
                if (status == 401 || status == 404) {
                    return null;
                }
                throw new IOException("Error al autenticar profesional: HTTP " + status + " - " + responseBody);
            }
        }
    }

    public List<clinica_dto> getClinicas() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/clinicas");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200 && response.getEntity() != null) {
                    String body = new String(response.getEntity().getContent().readAllBytes());
                    return parseClinicasFromJson(body);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ========== USUARIOS SALUD ==========

    public List<usuario_salud_dto> getAllUsuarios(String tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/usuarios?tenantId=" + tenantId);

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

    // ========== DOCUMENTOS CLÍNICOS ==========

    public documento_clinico_dto crearDocumento(
            String usuarioSaludCedula, Integer profesionalCi, String codigoMotivoConsulta,
            String descripcionDiagnostico, String fechaInicioDiagnostico, String codigoEstadoProblema,
            String codigoGradoCerteza, String fechaProximaConsulta, String descripcionProximaConsulta,
            String referenciaAlta, UUID tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(getBackendUrl() + "/documentos?tenantId=" + tenantId);

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
                    System.err.println("Error al crear documento: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<documento_clinico_dto> getDocumentosPorPaciente(String cedula, UUID tenantId) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/documentos/paciente/" + cedula + "?tenantId=" + tenantId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseDocumentosListFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public Map<String, String> getMotivosConsulta() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/documentos/catalogos/motivos");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedHashMap<>();
    }

    /**
     * Busca motivos de consulta por término (para autocompletado)
     */
    public Map<String, String> buscarMotivosConsulta(String termino) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String url = getBackendUrl() + "/documentos/catalogos/motivos/buscar?termino=" +
                        java.net.URLEncoder.encode(termino, "UTF-8");
            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedHashMap<>();
    }

    public Map<String, String> getEstadosProblema() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/documentos/catalogos/estados");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedHashMap<>();
    }

    public Map<String, String> getGradosCerteza() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/documentos/catalogos/grados-certeza");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseMapFromJson(responseBody);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedHashMap<>();
    }

    /**
     * Fuerza la sincronización inmediata de documentos pendientes con el componente central
     * NOTA: Este es un método provisional para testing/debugging
     */
    public boolean sincronizarPendientes() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(getBackendUrl() + "/documentos/sincronizar-pendientes");
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    System.out.println("Sincronización pendientes iniciada correctamente");
                    return true;
                } else {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    System.err.println("Error al sincronizar pendientes: " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error en sincronizarPendientes: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ========== PARSERS ==========

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

    private profesional_salud_dto parseProfesionalFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            profesional_salud_dto dto = new profesional_salud_dto();
            dto.setCi(jsonObject.getInt("ci", 0));
            dto.setNombre(jsonObject.getString("nombre", null));
            dto.setApellidos(jsonObject.getString("apellidos", null));
            dto.setEspecialidad(jsonObject.getString("especialidad", null));
            dto.setEmail(jsonObject.getString("email", null));
            dto.setTenantId(jsonObject.getString("tenantId", null));
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<clinica_dto> parseClinicasFromJson(String jsonString) {
        List<clinica_dto> clinicas = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray array = reader.readArray();
            for (JsonObject obj : array.getValuesAs(JsonObject.class)) {
                clinica_dto dto = new clinica_dto();
                dto.setTenantId(obj.getString("tenantId", null));
                dto.setNombre(obj.getString("nombre", dto.getTenantId()));
                clinicas.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clinicas;
    }

    private documento_clinico_dto parseDocumentoFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject json = reader.readObject();
            return parseDocumentoFromJsonObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<documento_clinico_dto> parseDocumentosListFromJson(String jsonString) {
        List<documento_clinico_dto> list = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject response = reader.readObject();
            JsonArray jsonArray = response.getJsonArray("data");

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject json = jsonArray.getJsonObject(i);
                documento_clinico_dto dto = parseDocumentoFromJsonObject(json);
                if (dto != null) {
                    list.add(dto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private documento_clinico_dto parseDocumentoFromJsonObject(JsonObject json) {
        try {
            documento_clinico_dto dto = new documento_clinico_dto();

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

    private Map<String, String> parseMapFromJson(String jsonString) {
        Map<String, String> map = new LinkedHashMap<>();
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
}
