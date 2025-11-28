package hcen.central.frontend.usuariosalud.service;

import hcen.central.frontend.usuariosalud.dto.UsuarioSaludDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
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
import java.io.Serializable;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de cliente REST para comunicación con el backend central
 */
@ApplicationScoped
public class APIService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(APIService.class.getName());

    // Fallback URLs si no se puede obtener desde web.xml
    private static final String BACKEND_URL_PROD = "https://hcen-uy.web.elasticloud.uy/";
    private static final String BACKEND_URL_DEV = "http://localhost:8080/";

    /**
     * Obtiene la URL del backend desde web.xml (parámetro hcen.backendUrl).
     * Si no está configurado, usa fallback dinámico basado en el servidor actual.
     * Si FacesContext no está disponible, usa constantes según app.environment.
     */
    private String getBackendUrl() {
        String url = null;

        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                // Intentar obtener URL configurada en web.xml
                url = facesContext.getExternalContext().getInitParameter("hcen.backendUrl");

                if (url != null && !url.isBlank()) {
                    LOGGER.fine("URL del backend obtenida desde web.xml: " + url);
                } else {
                    // Fallback: construir URL basándose en el contexto actual
                    String serverName = facesContext.getExternalContext().getRequestServerName();
                    int serverPort = facesContext.getExternalContext().getRequestServerPort();

                    if (serverPort == 80 || serverPort == 443) {
                        url = "http://" + serverName + "/";
                    } else {
                        url = "http://" + serverName + ":" + serverPort + "/";
                    }

                    LOGGER.warning("No se configuró hcen.backendUrl en web.xml, usando fallback dinámico: " + url);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al obtener URL desde FacesContext, usando fallback de constantes", e);
        }

        // Último fallback: usar constantes según app.environment
        if (url == null || url.isBlank()) {
            String env = System.getProperty("app.environment", "development");
            url = "production".equalsIgnoreCase(env) ? BACKEND_URL_PROD : BACKEND_URL_DEV;
            LOGGER.warning("FacesContext no disponible, usando URL desde constantes: " + url);
        }

        // Normalizar URL: eliminar trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
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
            throw new IllegalStateException("No se pudo inicializar el contexto SSL", e);
        }
    }

    /**
     * Obtiene el JWT token de la cookie
     */
    private String getJwtTokenFromCookie() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                if (request.getCookies() != null) {
                    for (Cookie cookie : request.getCookies()) {
                        if ("jwt_token".equals(cookie.getName())) {
                            return cookie.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al obtener JWT de cookie", e);
        }
        return null;
    }

    /**
     * Obtiene datos de un usuario por cédula
     * GET /api/usuarios/{cedula}
     */
    public UsuarioSaludDTO obtenerUsuario(String cedula) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(getBackendUrl() + "/api/usuarios/" + cedula);
            request.setHeader("Content-Type", "application/json");
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                request.setHeader("Authorization", "Bearer " + jwtToken);
                LOGGER.info("[APIService] Enviando JWT en Authorization Header, token=" + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
            } else {
                LOGGER.warning("[APIService] NO hay JWT disponible para enviar en la petición");
            }

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseUsuarioFromJson(responseBody);
                } else if (response.getCode() == 404) {
                    return null; // Usuario no encontrado
                } else {
                    throw new IOException("Error al obtener usuario: HTTP " + response.getCode());
                }
            }
        }
    }

    /**
     * Actualiza datos de contacto y preferencias de un usuario
     * PUT /api/usuarios/{cedula}
     */
    public UsuarioSaludDTO actualizarUsuario(String cedula,
                                             String email,
                                             String telefono,
                                             String direccion,
                                             Boolean notificacionesHabilitadas) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPut request = new HttpPut(getBackendUrl() + "/api/usuarios/" + cedula);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                request.setHeader("Authorization", "Bearer " + jwtToken);
                LOGGER.info("[APIService] Enviando JWT en Authorization Header para PUT, token=" + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
            } else {
                LOGGER.warning("[APIService] NO hay JWT disponible para enviar en PUT");
            }

            JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("email", email != null ? email : "")
                .add("telefono", telefono != null ? telefono : "")
                .add("direccion", direccion != null ? direccion : "");

            if (notificacionesHabilitadas != null) {
                builder.add("notificacionesHabilitadas", notificacionesHabilitadas);
            }

            JsonObject updateData = builder.build();

            StringEntity entity = new StringEntity(updateData.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseUsuarioFromJson(responseBody);
                } else {
                    String errorBody = new String(response.getEntity().getContent().readAllBytes());
                    throw new IOException("Error al actualizar usuario: HTTP " + response.getCode() + " - " + errorBody);
                }
            }
        }
    }

    /**
     * Parsea JSON del backend a DTO frontend
     */
    private UsuarioSaludDTO parseUsuarioFromJson(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();

            UsuarioSaludDTO dto = new UsuarioSaludDTO();
            dto.setId(obj.getJsonNumber("id").longValue());
            dto.setCedula(obj.getString("cedula"));
            dto.setPrimerNombre(obj.getString("primerNombre", null));
            dto.setSegundoNombre(obj.getString("segundoNombre", null));
            dto.setPrimerApellido(obj.getString("primerApellido", null));
            dto.setSegundoApellido(obj.getString("segundoApellido", null));
            dto.setEmail(obj.getString("email", null));
            dto.setTelefono(obj.getString("telefono", null));
            dto.setDireccion(obj.getString("direccion", null));
            dto.setNombreCompleto(obj.getString("nombreCompleto", null));
            dto.setActive(obj.getBoolean("active", true));
            if (obj.containsKey("notificacionesHabilitadas") && !obj.isNull("notificacionesHabilitadas")) {
                dto.setNotificacionesHabilitadas(obj.getBoolean("notificacionesHabilitadas", true));
            }

            // Fecha de nacimiento (puede venir como string ISO "2000-01-15" o como array [2000,1,15])
            if (obj.containsKey("fechaNacimiento") && !obj.isNull("fechaNacimiento")) {
                try {
                    JsonValue fechaValue = obj.get("fechaNacimiento");

                    switch (fechaValue.getValueType()) {
                        case STRING:
                            // Formato ISO string "2000-01-15" - caso esperado
                            String fechaStr = obj.getString("fechaNacimiento");
                            dto.setFechaNacimiento(LocalDate.parse(fechaStr));
                            break;

                        case ARRAY:
                            // Formato array [year, month, day] - serialización por defecto de LocalDate
                            var fechaArray = obj.getJsonArray("fechaNacimiento");
                            if (fechaArray.size() >= 3) {
                                int year = fechaArray.getInt(0);
                                int month = fechaArray.getInt(1);
                                int day = fechaArray.getInt(2);
                                dto.setFechaNacimiento(LocalDate.of(year, month, day));
                                LOGGER.fine("fechaNacimiento parseada desde array: " + year + "-" + month + "-" + day);
                            } else {
                                LOGGER.warning("fechaNacimiento es un array pero no tiene 3 elementos: " + fechaArray);
                                dto.setFechaNacimiento(null);
                            }
                            break;

                        case OBJECT:
                            // Formato objeto {year: 2000, month: 1, day: 15}
                            JsonObject fechaObj = obj.getJsonObject("fechaNacimiento");
                            if (fechaObj.containsKey("year") && fechaObj.containsKey("month") && fechaObj.containsKey("day")) {
                                int year = fechaObj.getInt("year");
                                int month = fechaObj.getInt("month");
                                int day = fechaObj.getInt("day");
                                dto.setFechaNacimiento(LocalDate.of(year, month, day));
                            } else {
                                LOGGER.warning("fechaNacimiento es un objeto con estructura desconocida: " + fechaObj);
                                dto.setFechaNacimiento(null);
                            }
                            break;

                        default:
                            LOGGER.warning("fechaNacimiento tiene tipo desconocido: " + fechaValue.getValueType());
                            dto.setFechaNacimiento(null);
                            break;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear fechaNacimiento, ignorando campo", e);
                    dto.setFechaNacimiento(null);
                }
            }

            // Tipo de documento (puede venir como string, objeto o array del enum)
            if (obj.containsKey("tipoDocumento") && !obj.isNull("tipoDocumento")) {
                try {
                    JsonValue tipoDocValue = obj.get("tipoDocumento");

                    // Verificar el tipo del valor antes de intentar parsearlo
                    switch (tipoDocValue.getValueType()) {
                        case STRING:
                            // Es un string simple - caso esperado
                            dto.setTipoDocumento(obj.getString("tipoDocumento"));
                            break;

                        case OBJECT:
                            // Es un objeto complejo (enum serializado con propiedades)
                            JsonObject tipoDocObj = obj.getJsonObject("tipoDocumento");
                            if (tipoDocObj.containsKey("name")) {
                                dto.setTipoDocumento(tipoDocObj.getString("name"));
                            } else if (tipoDocObj.containsKey("codigo")) {
                                dto.setTipoDocumento(tipoDocObj.getString("codigo"));
                            } else {
                                LOGGER.warning("tipoDocumento es un objeto con estructura desconocida: " + tipoDocObj.toString());
                                dto.setTipoDocumento(null);
                            }
                            break;

                        case ARRAY:
                            // Es un array - posiblemente una serialización incorrecta del enum
                            LOGGER.warning("tipoDocumento vino como array JSON (inesperado), ignorando: " + tipoDocValue.toString());
                            dto.setTipoDocumento(null);
                            break;

                        default:
                            LOGGER.warning("tipoDocumento tiene tipo desconocido: " + tipoDocValue.getValueType());
                            dto.setTipoDocumento(null);
                            break;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear tipoDocumento, ignorando campo", e);
                    dto.setTipoDocumento(null);
                }
            }

            return dto;
        }
    }
}
