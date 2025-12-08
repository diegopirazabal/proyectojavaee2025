package hcen.frontend.admin.service;

import hcen.frontend.admin.dto.admin_hcen_dto;
import hcen.frontend.admin.dto.clinica_dto;
import hcen.frontend.admin.dto.clinica_form;
import hcen.frontend.admin.dto.prestador_dto;
import hcen.frontend.admin.dto.prestador_form;
import hcen.frontend.admin.dto.usuario_salud_dto;
import hcen.frontend.admin.dto.usuario_sistema_dto;
import hcen.frontend.admin.dto.reportes_estadisticas_dto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@ApplicationScoped
public class api_service {

    private static final String CENTRAL_ENV_VAR = "HCEN_API_BASE_URL";
    private static final String CENTRAL_SYS_PROP = "hcen.apiBaseUrl";
    // URLs por defecto para desarrollo y producción
    private static final String DEFAULT_BACKEND_URL_DEV = "http://localhost:8080/api";
    private static final String DEFAULT_BACKEND_URL_PROD = "https://hcen-uy.web.elasticloud.uy/api";

    private static final String PERIPHERAL_ENV_VAR = "HCEN_PERIPHERAL_API_BASE_URL";
    private static final String PERIPHERAL_SYS_PROP = "hcen.peripheralApiBaseUrl";
    private static final String DEFAULT_PERIPHERAL_URL = "http://localhost:8080/multitenant-api/";

    private static final Logger LOGGER = Logger.getLogger(api_service.class.getName());

    // CookieStore compartido para almacenar JWT entre requests HTTP
    // IMPORTANTE: No usar 'final' porque HttpClient necesita inyectarlo al construirse
    private final org.apache.hc.client5.http.cookie.CookieStore cookieStore = new org.apache.hc.client5.http.cookie.BasicCookieStore();

    // URL resuelta dinámicamente en cada petición para soportar cambio de contexto
    private final String peripheralUrl = resolvePeripheralUrl();

    private String getBackendUrl() {
        return resolveBackendUrl();
    }

    public admin_hcen_dto authenticate(String username, String password) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(getBackendUrl() + "/auth/login");

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
            HttpPost request = new HttpPost(getBackendUrl() + "/notifications/broadcast-test");
            request.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));
            attachAuthorizationHeader(request);

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
            String url = getBackendUrl() + "/usuarios-salud";
            LOGGER.info("═══════════════════════════════════════════════════════");
            LOGGER.info("[obtenerUsuariosSalud] URL completa: " + url);
            HttpGet request = new HttpGet(url);
            attachAuthorizationHeader(request);
            LOGGER.info("[obtenerUsuariosSalud] Headers del request: " + java.util.Arrays.toString(request.getHeaders()));
            LOGGER.info("═══════════════════════════════════════════════════════");
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

    public List<usuario_sistema_dto> obtenerUsuariosSistema(String tipoDoc,
                                                            String numeroDoc,
                                                            String nombre,
                                                            String apellido) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            StringBuilder urlBuilder = new StringBuilder(getBackendUrl()).append("/usuarios-sistema");
            List<String> params = new ArrayList<>();
            if (tipoDoc != null && !tipoDoc.isBlank()) {
                params.add("tipoDoc=" + encode(tipoDoc));
            }
            if (numeroDoc != null && !numeroDoc.isBlank()) {
                params.add("numeroDoc=" + encode(numeroDoc));
            }
            if (nombre != null && !nombre.isBlank()) {
                params.add("nombre=" + encode(nombre));
            }
            if (apellido != null && !apellido.isBlank()) {
                params.add("apellido=" + encode(apellido));
            }
            params.add("limit=150");
            if (!params.isEmpty()) {
                urlBuilder.append('?').append(String.join("&", params));
            }

            HttpGet request = new HttpGet(urlBuilder.toString());
            LOGGER.fine(() -> "GET " + request.getRequestUri());
            attachAuthorizationHeader(request);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                LOGGER.fine(() -> "Respuesta usuarios-sistema status=" + status);
                if (status == 200) {
                    return parseUsuariosSistema(readEntityContent(response));
                }
                if (status == 401 || status == 403) {
                    String body = readEntityContent(response);
                    LOGGER.warning(() -> "Catálogo de usuarios rechazado por autorización. Código=" + status);
                    throw new ApiUnauthorizedException(
                            body != null && !body.isBlank()
                                    ? "Acceso no autorizado: " + body
                                    : "Acceso no autorizado al catálogo de usuarios");
                }
                throw new IOException("Código inesperado al obtener catálogo de usuarios: " + status);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener catálogo de usuarios", e);
            throw new RuntimeException("No se pudo obtener el catálogo de usuarios", e);
        }
    }

    public boolean enviarNotificacionUsuario(String cedula, String mensaje) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(getBackendUrl() + "/notifications/usuarios/" + cedula);
            attachAuthorizationHeader(request);

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

    public boolean actualizarUsuarioSalud(usuario_sistema_dto usuario) {
        if (usuario == null || usuario.getNumeroDocumento() == null || usuario.getNumeroDocumento().isBlank()) {
            return false;
        }
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(getBackendUrl() + "/usuarios-salud/" + usuario.getNumeroDocumento());
            attachAuthorizationHeader(request);

            var builder = Json.createObjectBuilder()
                .add("primerNombre", required(usuario.getPrimerNombre(), "El nombre es requerido"))
                .add("primerApellido", required(usuario.getPrimerApellido(), "El apellido es requerido"))
                .add("email", required(usuario.getEmail(), "El email es requerido"));

            if (usuario.getSegundoNombre() != null) {
                builder.add("segundoNombre", usuario.getSegundoNombre());
            } else {
                builder.addNull("segundoNombre");
            }
            if (usuario.getSegundoApellido() != null) {
                builder.add("segundoApellido", usuario.getSegundoApellido());
            } else {
                builder.addNull("segundoApellido");
            }
            if (usuario.getActivo() != null) {
                builder.add("activo", usuario.getActivo());
            }
            if (usuario.getFechaNacimiento() != null && !usuario.getFechaNacimiento().isBlank()) {
                builder.add("fechaNacimiento", usuario.getFechaNacimiento());
            }
            if (usuario.getTenantId() != null && !usuario.getTenantId().isBlank()) {
                builder.add("tenantId", usuario.getTenantId());
            }

            request.setEntity(new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar usuario de salud", e);
            return false;
        }
    }

    public boolean actualizarProfesional(usuario_sistema_dto usuario) {
        if (usuario == null || usuario.getNumeroDocumento() == null || usuario.getNumeroDocumento().isBlank()) {
            return false;
        }
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(peripheralUrl + "/profesionales/" + usuario.getNumeroDocumento());
            var builder = Json.createObjectBuilder()
                .add("nombre", required(usuario.getPrimerNombre(), "El nombre del profesional es requerido"))
                .add("apellidos", required(usuario.getPrimerApellido(), "Los apellidos son requeridos"));

            if (usuario.getEspecialidad() != null) {
                builder.add("especialidad", usuario.getEspecialidad());
            } else {
                builder.addNull("especialidad");
            }
            if (usuario.getEmail() != null) {
                builder.add("email", usuario.getEmail());
            } else {
                builder.addNull("email");
            }

            request.setEntity(new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar profesional de salud", e);
            return false;
        }
    }

    public boolean actualizarAdministradorClinica(usuario_sistema_dto usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getId().isBlank()) {
            return false;
        }
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(peripheralUrl + "/administradores/" + usuario.getId());
            var builder = Json.createObjectBuilder()
                .add("username", required(usuario.getUsername(), "El usuario es requerido"))
                .add("nombre", required(usuario.getPrimerNombre(), "El nombre es requerido"))
                .add("apellidos", required(usuario.getPrimerApellido(), "Los apellidos son requeridos"));

            if (usuario.getTenantId() != null && !usuario.getTenantId().isBlank()) {
                builder.add("tenantId", usuario.getTenantId());
            }

            request.setEntity(new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar administrador de clínica", e);
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

    public reportes_estadisticas_dto obtenerReportesEstadisticas() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(peripheralUrl + "/reportes/estadisticas");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    return parseReportesEstadisticas(responseBody);
                }
                throw new IOException("Código inesperado al obtener las estadísticas: " + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener estadísticas de reportes", e);
            throw new RuntimeException("No se pudo obtener el resumen de reportes", e);
        }
    }

    /**
     * Obtiene estadísticas de reportes con paginación de clínicas (lazy loading)
     *
     * @param page Número de página (0-indexed)
     * @param pageSize Cantidad de clínicas por página
     * @return DTO con totales globales y clínicas de la página solicitada
     */
    public reportes_estadisticas_dto obtenerReportesEstadisticasPaginadas(int page, int pageSize) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String url = peripheralUrl + "/reportes/estadisticas/paginado"
                + "?page=" + page
                + "&size=" + pageSize;

            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    return parseReportesEstadisticas(responseBody);
                }
                throw new IOException("Código inesperado al obtener estadísticas paginadas: "
                    + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener estadísticas paginadas", e);
            throw new RuntimeException("No se pudo obtener estadísticas paginadas", e);
        }
    }

    /**
     * Obtiene el total de accesos aprobados desde el backend central
     *
     * @return Cantidad de permisos activos
     */
    public long obtenerAccesosAprobadosCentral() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String url = getBackendUrl() + "/reportes/accesos-aprobados";

            HttpGet request = new HttpGet(url);
            attachAuthorizationHeader(request);

            LOGGER.log(Level.INFO, "Obteniendo accesos aprobados desde central: {0}", url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    // Parse JSON: {"count": 123}
                    try (JsonReader jsonReader = Json.createReader(
                            new StringReader(responseBody))) {
                        JsonObject json = jsonReader.readObject();
                        return json.getJsonNumber("count").longValue();
                    }
                }
                throw new IOException("Error al obtener accesos aprobados: " + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener accesos aprobados desde central", e);
            return 0L; // Retornar 0 en caso de error
        }
    }

    /**
     * Obtiene accesos aprobados agrupados por clínica desde el backend central
     *
     * @return Map con tenantId como clave y cantidad de accesos como valor
     */
    public Map<String, Long> obtenerAccesosPorClinicaCentral() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String url = getBackendUrl() + "/reportes/accesos-aprobados-por-clinica";

            HttpGet request = new HttpGet(url);
            attachAuthorizationHeader(request);

            LOGGER.log(Level.INFO, "Obteniendo accesos por clínica desde central: {0}", url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = readEntityContent(response);
                    // Parse JSON: {"uuid1": 5, "uuid2": 3, ...}
                    Map<String, Long> resultado = new HashMap<>();
                    try (JsonReader jsonReader = Json.createReader(
                            new StringReader(responseBody))) {
                        JsonObject json = jsonReader.readObject();
                        for (String tenantId : json.keySet()) {
                            resultado.put(tenantId, json.getJsonNumber(tenantId).longValue());
                        }
                    }
                    return resultado;
                }
                throw new IOException("Error al obtener accesos por clínica: " + response.getCode());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener accesos por clínica desde central", e);
            return Collections.emptyMap(); // Retornar mapa vacío en caso de error
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
        String apiBase = getBackendUrl();
        String baseUrl = apiBase.endsWith("/api") ? apiBase.substring(0, apiBase.length() - 4) : apiBase;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String redirectUri = baseUrl + "/api/auth/callback";

        try {
            return baseUrl + "/api/auth/login?redirect_uri=" + java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&origin=admin";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error encoding redirect_uri", e);
            return baseUrl + "/api/auth/login?redirect_uri=" + redirectUri + "&origin=admin";
        }
    }

    public boolean isBackendAvailable() {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(getBackendUrl() + "/health");
            attachAuthorizationHeader(request);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getCode() == 200;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ============= Métodos para gestión de políticas de acceso =============

    /**
     * Obtiene el ID de la historia clínica de un paciente por su cédula
     */
    public hcen.frontend.admin.dto.historia_clinica_id_response obtenerHistoriaIdPorCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/historia-clinica/by-cedula/" + cedula.trim());
            attachAuthorizationHeader(request);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                String body = readEntityContent(response);

                if (status == 200) {
                    try (JsonReader reader = Json.createReader(new StringReader(body))) {
                        JsonObject wrapper = reader.readObject();
                        if (wrapper.containsKey("data") && !wrapper.isNull("data")) {
                            JsonObject data = wrapper.getJsonObject("data");
                            hcen.frontend.admin.dto.historia_clinica_id_response dto =
                                new hcen.frontend.admin.dto.historia_clinica_id_response();
                            dto.setHistoriaId(data.getString("historiaId"));
                            dto.setCedula(data.getString("cedula"));
                            return dto;
                        }
                    }
                } else if (status == 404) {
                    return null; // No existe historia clínica para esa cédula
                }

                LOGGER.warning(() -> "Error al obtener historiaId: status=" + status + ", body=" + body);
                return null;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener historia clínica por cédula", e);
            throw new RuntimeException("Error al obtener historia clínica", e);
        }
    }

    /**
     * Lista todas las políticas de acceso de una historia clínica
     */
    public List<hcen.frontend.admin.dto.politica_acceso_dto> listarPermisosPorHistoria(String historiaId) {
        if (historiaId == null || historiaId.isBlank()) {
            throw new IllegalArgumentException("El ID de historia clínica es requerido");
        }

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/politicas-acceso/historia/" + historiaId);
            attachAuthorizationHeader(request);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                String body = readEntityContent(response);

                if (status == 200) {
                    return parsePoliticasAcceso(body);
                }

                LOGGER.warning(() -> "Error al listar permisos: status=" + status + ", body=" + body);
                return new ArrayList<>();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al listar permisos", e);
            return new ArrayList<>();
        }
    }

    /**
     * Extiende la fecha de expiración de un permiso
     */
    public boolean extenderExpiracionPermiso(String permisoId, String nuevaFechaExpiracion) {
        if (permisoId == null || permisoId.isBlank()) {
            throw new IllegalArgumentException("El ID del permiso es requerido");
        }
        if (nuevaFechaExpiracion == null || nuevaFechaExpiracion.isBlank()) {
            throw new IllegalArgumentException("La nueva fecha de expiración es requerida");
        }

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(getBackendUrl() + "/politicas-acceso/" + permisoId + "/extender");
            attachAuthorizationHeader(request);

            JsonObject requestBody = Json.createObjectBuilder()
                .add("nuevaFechaExpiracion", nuevaFechaExpiracion)
                .build();

            request.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                if (status == 200) {
                    return true;
                }

                String body = readEntityContent(response);
                LOGGER.warning(() -> "Error al extender expiración: status=" + status + ", body=" + body);
                return false;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al extender expiración del permiso", e);
            return false;
        }
    }

    /**
     * Modifica el tipo de permiso de una política de acceso
     */
    public boolean modificarTipoPermiso(String permisoId, String tipoPermiso,
                                        Integer ciProfesional, String especialidad) {
        if (permisoId == null || permisoId.isBlank()) {
            throw new IllegalArgumentException("El ID del permiso es requerido");
        }
        if (tipoPermiso == null || tipoPermiso.isBlank()) {
            throw new IllegalArgumentException("El tipo de permiso es requerido");
        }

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(getBackendUrl() + "/politicas-acceso/" + permisoId + "/modificar-tipo");
            attachAuthorizationHeader(request);

            var builder = Json.createObjectBuilder()
                .add("tipoPermiso", tipoPermiso);

            if (ciProfesional != null) {
                builder.add("ciProfesional", ciProfesional);
            }
            if (especialidad != null && !especialidad.isBlank()) {
                builder.add("especialidad", especialidad);
            }

            request.setEntity(new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                if (status == 200) {
                    return true;
                }

                String body = readEntityContent(response);
                LOGGER.warning(() -> "Error al modificar tipo de permiso: status=" + status + ", body=" + body);
                return false;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al modificar tipo de permiso", e);
            return false;
        }
    }

    /**
     * Revoca un permiso de acceso
     */
    public boolean revocarPermiso(String permisoId, String motivo) {
        if (permisoId == null || permisoId.isBlank()) {
            throw new IllegalArgumentException("El ID del permiso es requerido");
        }

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(getBackendUrl() + "/politicas-acceso/" + permisoId + "/revocar");
            attachAuthorizationHeader(request);

            var builder = Json.createObjectBuilder();
            if (motivo != null && !motivo.isBlank()) {
                builder.add("motivo", motivo);
            }

            request.setEntity(new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                if (status == 200) {
                    return true;
                }

                String body = readEntityContent(response);
                LOGGER.warning(() -> "Error al revocar permiso: status=" + status + ", body=" + body);
                return false;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al revocar permiso", e);
            return false;
        }
    }

    /**
     * Parsea una lista de políticas de acceso desde JSON
     */
    private List<hcen.frontend.admin.dto.politica_acceso_dto> parsePoliticasAcceso(String jsonString) {
        List<hcen.frontend.admin.dto.politica_acceso_dto> politicas = new ArrayList<>();

        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject wrapper = reader.readObject();

            if (wrapper.containsKey("data") && !wrapper.isNull("data")) {
                JsonArray array = wrapper.getJsonArray("data");

                for (JsonValue value : array) {
                    if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                        continue;
                    }

                    JsonObject obj = value.asJsonObject();
                    hcen.frontend.admin.dto.politica_acceso_dto dto =
                        new hcen.frontend.admin.dto.politica_acceso_dto();

                    dto.setId(obj.getString("id", null));
                    dto.setHistoriaClinicaId(obj.getString("historiaClinicaId", null));
                    dto.setDocumentoId(obj.getString("documentoId", null));
                    dto.setTipoPermiso(obj.getString("tipoPermiso", null));
                    dto.setTenantId(obj.getString("tenantId", null));
                    dto.setEstado(obj.getString("estado", null));

                    if (!obj.isNull("ciProfesional")) {
                        dto.setCiProfesional(obj.getInt("ciProfesional"));
                    }
                    if (!obj.isNull("especialidad")) {
                        dto.setEspecialidad(obj.getString("especialidad"));
                    }
                    if (!obj.isNull("fechaOtorgamiento")) {
                        dto.setFechaOtorgamiento(LocalDateTime.parse(
                            obj.getString("fechaOtorgamiento"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }
                    if (!obj.isNull("fechaExpiracion")) {
                        dto.setFechaExpiracion(LocalDateTime.parse(
                            obj.getString("fechaExpiracion"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }
                    if (!obj.isNull("motivoRevocacion")) {
                        dto.setMotivoRevocacion(obj.getString("motivoRevocacion"));
                    }
                    if (!obj.isNull("fechaRevocacion")) {
                        dto.setFechaRevocacion(LocalDateTime.parse(
                            obj.getString("fechaRevocacion"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }

                    politicas.add(dto);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parseando políticas de acceso", e);
        }

        return politicas;
    }

    // ============= Fin métodos de gestión de políticas de acceso =============

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
                dto.setCedula(readStringValue(obj, "cedula"));
                dto.setNombreCompleto(readStringValue(obj, "nombreCompleto"));
                dto.setPrimerNombre(readStringValue(obj, "primerNombre"));
                dto.setPrimerApellido(readStringValue(obj, "primerApellido"));
                dto.setEmail(readStringValue(obj, "email"));
                if (obj.containsKey("active") && !obj.isNull("active")) {
                    dto.setActive(obj.getBoolean("active"));
                }
                if (obj.containsKey("createdAt") && !obj.isNull("createdAt")) {
                    dto.setCreatedAt(readInstant(obj.get("createdAt")));
                }
                if (obj.containsKey("lastLogin") && !obj.isNull("lastLogin")) {
                    dto.setLastLogin(readInstant(obj.get("lastLogin")));
                }

                usuarios.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing usuarios de salud payload", e);
            throw new RuntimeException("No se pudo interpretar la lista de usuarios de salud", e);
        }
        return usuarios;
    }

    private String readStringValue(JsonObject obj, String key) {
        if (!obj.containsKey(key) || obj.isNull(key)) {
            return null;
        }
        JsonValue value = obj.get(key);
        if (value == null) {
            return null;
        }
        return switch (value.getValueType()) {
            case STRING -> ((JsonString) value).getString();
            case NUMBER -> ((JsonNumber) value).toString();
            case TRUE -> "true";
            case FALSE -> "false";
            default -> value.toString();
        };
    }

    private Instant readInstant(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            return null;
        }
        try {
            return switch (value.getValueType()) {
                case STRING -> {
                    String text = ((JsonString) value).getString();
                    yield (text == null || text.isBlank()) ? null : Instant.parse(text);
                }
                case NUMBER -> {
                    JsonNumber number = (JsonNumber) value;
                    long rawValue = number.longValue();
                    // Heuristic: values greater than epoch seconds range are milliseconds.
                    Instant instant = rawValue > 1_000_000_0000L
                            ? Instant.ofEpochMilli(rawValue)
                            : Instant.ofEpochSecond(rawValue);
                    yield instant;
                }
                default -> null;
            };
        } catch (Exception parseError) {
            LOGGER.log(Level.WARNING, "No se pudo interpretar fecha JSON: {0}", value);
            return null;
        }
    }

    private List<usuario_sistema_dto> parseUsuariosSistema(String jsonString) {
        List<usuario_sistema_dto> usuarios = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                JsonObject obj = value.asJsonObject();
                usuario_sistema_dto dto = new usuario_sistema_dto();
                dto.setTipoUsuario(getString(obj, "tipo"));
                dto.setOrigen(getString(obj, "origen"));
                dto.setId(getString(obj, "id"));
                dto.setTipoDocumento(getString(obj, "tipoDocumento"));
                dto.setNumeroDocumento(getString(obj, "numeroDocumento"));
                dto.setPrimerNombre(getString(obj, "primerNombre"));
                dto.setSegundoNombre(getString(obj, "segundoNombre"));
                dto.setPrimerApellido(getString(obj, "primerApellido"));
                dto.setSegundoApellido(getString(obj, "segundoApellido"));
                dto.setNombreCompleto(getString(obj, "nombreCompleto"));
                dto.setEmail(getString(obj, "email"));
                dto.setActivo(getBoolean(obj, "activo"));
                dto.setFechaNacimiento(getString(obj, "fechaNacimiento"));
                dto.setEspecialidad(getString(obj, "especialidad"));
                dto.setTenantId(getString(obj, "tenantId"));
                dto.setUsername(getString(obj, "username"));
                usuarios.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing catálogo de usuarios", e);
            throw new RuntimeException("No se pudo interpretar el catálogo de usuarios", e);
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

    private reportes_estadisticas_dto parseReportesEstadisticas(String jsonString) {
        reportes_estadisticas_dto resultado = new reportes_estadisticas_dto();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject root = reader.readObject();
            resultado.setGeneratedAt(getString(root, "generatedAt"));
            resultado.setTotalClinicas(root.getInt("totalClinicas", 0));

            if (root.containsKey("totals") && !root.isNull("totals")) {
                JsonObject totalsObj = root.getJsonObject("totals");
                reportes_estadisticas_dto.Totales totales = new reportes_estadisticas_dto.Totales();
                totales.setPacientes(getLong(totalsObj, "pacientes"));
                totales.setDocumentos(getLong(totalsObj, "documentos"));
                totales.setAccesosDocumentos(getLong(totalsObj, "accesosDocumentos"));
                totales.setProfesionales(getLong(totalsObj, "profesionales"));
                resultado.setTotals(totales);
            }

            if (root.containsKey("clinicas") && !root.isNull("clinicas")) {
                JsonArray clinicasArray = root.getJsonArray("clinicas");
                List<reportes_estadisticas_dto.ClinicaEstadistica> clinicas = new ArrayList<>();
                for (JsonValue value : clinicasArray) {
                    if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                        continue;
                    }
                    JsonObject clinicaObj = value.asJsonObject();
                    reportes_estadisticas_dto.ClinicaEstadistica dto =
                        new reportes_estadisticas_dto.ClinicaEstadistica();
                    dto.setTenantId(getString(clinicaObj, "tenantId"));
                    dto.setNombre(getString(clinicaObj, "nombre"));
                    dto.setEmail(getString(clinicaObj, "email"));
                    dto.setPacientes(getLong(clinicaObj, "pacientes"));
                    dto.setDocumentos(getLong(clinicaObj, "documentos"));
                    dto.setAccesosDocumentos(getLong(clinicaObj, "accesosDocumentos"));
                    dto.setProfesionales(getLong(clinicaObj, "profesionales"));
                    clinicas.add(dto);
                }
                resultado.setClinicas(clinicas);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parseando las estadísticas de reportes", e);
            throw new RuntimeException("No se pudo interpretar el resumen de estadísticas", e);
        }
        return resultado;
    }

    private String getString(JsonObject obj, String key) {
        return obj.containsKey(key) && !obj.isNull(key) ? obj.getString(key) : null;
    }

    private long getLong(JsonObject obj, String key) {
        if (obj.containsKey(key) && !obj.isNull(key)) {
            JsonValue value = obj.get(key);
            if (value.getValueType() == JsonValue.ValueType.NUMBER) {
                return obj.getJsonNumber(key).longValue();
            }
        }
        return 0L;
    }

    private Boolean getBoolean(JsonObject obj, String key) {
        if (obj.containsKey(key) && !obj.isNull(key)) {
            JsonValue value = obj.get(key);
            if (value.getValueType() == JsonValue.ValueType.TRUE) {
                return Boolean.TRUE;
            }
            if (value.getValueType() == JsonValue.ValueType.FALSE) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
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

    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // no-op to accept any client cert
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // no-op to accept any server cert
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private CloseableHttpClient createHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUST_ALL_MANAGER}, null);

            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            return HttpClients.custom()
                    .setConnectionManager(
                            PoolingHttpClientConnectionManagerBuilder.create()
                                    .setSSLSocketFactory(sslSocketFactory)
                                    .build())
                    .setDefaultCookieStore(cookieStore)  // Usar CookieStore compartido para persistir JWT
                    .build();
        } catch (GeneralSecurityException e) {
            LOGGER.log(Level.WARNING, "Falling back to default HttpClient without relaxed SSL", e);
            return HttpClients.custom()
                    .setDefaultCookieStore(cookieStore)  // Usar CookieStore compartido incluso en fallback
                    .build();
        }
    }

    private String resolveBackendUrl() {
        // 1. Primero intentar variable de entorno
        String envValue = System.getenv(CENTRAL_ENV_VAR);
        if (envValue != null && !envValue.isBlank()) {
            return sanitizeBaseUrl(envValue);
        }
        // 2. Luego propiedad de sistema
        String sysPropValue = System.getProperty(CENTRAL_SYS_PROP);
        if (sysPropValue != null && !sysPropValue.isBlank()) {
            return sanitizeBaseUrl(sysPropValue);
        }
        // 3. Detección automática basada en el contexto de la request
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                String serverName = facesContext.getExternalContext().getRequestServerName();
                // Si el servidor es localhost o 127.0.0.1, estamos en desarrollo
                if ("localhost".equalsIgnoreCase(serverName) || "127.0.0.1".equals(serverName)) {
                    LOGGER.fine("Detectado entorno de desarrollo, usando URL: " + DEFAULT_BACKEND_URL_DEV);
                    return sanitizeBaseUrl(DEFAULT_BACKEND_URL_DEV);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo determinar el servidor actual", e);
        }
        // 4. Por defecto usar producción
        LOGGER.fine("Usando URL de producción: " + DEFAULT_BACKEND_URL_PROD);
        return sanitizeBaseUrl(DEFAULT_BACKEND_URL_PROD);
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

    private void attachAuthorizationHeader(ClassicHttpRequest request) {
        if (request == null) {
            return;
        }
        String token = resolveJwtToken();
        if (token != null && !token.isBlank()) {
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            LOGGER.info("[api_service] Enviando JWT en Authorization Header, token=" + token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            LOGGER.warning("[api_service] NO hay JWT disponible para enviar en la petición");
        }
    }

    private String resolveJwtToken() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext == null) {
                return null;
            }

            Object requestObj = externalContext.getRequest();
            if (requestObj instanceof HttpServletRequest httpRequest) {
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("jwt_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                            return cookie.getValue();
                        }
                    }
                }
            }

            Object tokenAttr = externalContext.getSessionMap().get("jwtToken");
            if (tokenAttr instanceof String tokenStr && !tokenStr.isBlank()) {
                return tokenStr;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo resolver JWT para las llamadas al backend", e);
        }
        return null;
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
