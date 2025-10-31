package hcen.central.inus.service;

import hcen.central.inus.dto.UsuarioSistemaResponse;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.enums.UsuarioSistemaTipo;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PerifericoUsuariosClient {

    private static final Logger LOGGER = Logger.getLogger(PerifericoUsuariosClient.class.getName());

    private static final String PERIPHERAL_ENV_VAR = "HCEN_PERIPHERAL_API_BASE_URL";
    private static final String PERIPHERAL_SYS_PROP = "hcen.peripheralApiBaseUrl";
    private static final String DEFAULT_PERIPHERAL_URL = "http://179.31.3.190/multitenant-api";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(DEFAULT_TIMEOUT)
        .build();

    private final String peripheralBaseUrl = resolvePeripheralUrl();

    public List<UsuarioSistemaResponse> listarProfesionales(String numeroDocumento,
                                                            TipoDocumento tipoDocumento,
                                                            String nombre,
                                                            String apellido,
                                                            int limit) {
        try {
            StringBuilder urlBuilder = new StringBuilder(peripheralBaseUrl).append("/profesionales?");

            boolean hasParam = false;

            if (numeroDocumento != null && !numeroDocumento.isBlank()) {
                Optional<Integer> ci = parseInteger(numeroDocumento);
                if (ci.isPresent()) {
                    urlBuilder.append("ci=").append(ci.get());
                    hasParam = true;
                } else {
                    LOGGER.warning(() -> "Número de documento inválido para profesional: " + numeroDocumento);
                }
            }

            String search = buildSearchTerm(nombre, apellido);
            if (search != null) {
                if (hasParam) {
                    urlBuilder.append('&');
                }
                urlBuilder.append("search=").append(encode(search));
                hasParam = true;
            }

            if (limit > 0) {
                if (hasParam) {
                    urlBuilder.append('&');
                }
                urlBuilder.append("size=").append(limit);
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .timeout(DEFAULT_TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseProfesionales(response.body());
            } else {
                LOGGER.log(Level.WARNING, "Falló obtención de profesionales. Código {0}: {1}",
                    new Object[]{response.statusCode(), response.body()});
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener profesionales desde periférico", e);
        }
        return new ArrayList<>();
    }

    public List<UsuarioSistemaResponse> listarAdministradores(String nombre, String apellido, int limit) {
        try {
            StringBuilder urlBuilder = new StringBuilder(peripheralBaseUrl).append("/administradores?");
            boolean hasParam = false;

            String search = buildSearchTerm(nombre, apellido);
            if (search != null) {
                urlBuilder.append("search=").append(encode(search));
                hasParam = true;
            }

            if (limit > 0) {
                if (hasParam) {
                    urlBuilder.append('&');
                }
                urlBuilder.append("size=").append(limit);
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .timeout(DEFAULT_TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseAdministradores(response.body());
            } else {
                LOGGER.log(Level.WARNING, "Falló obtención de administradores. Código {0}: {1}",
                    new Object[]{response.statusCode(), response.body()});
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener administradores desde periférico", e);
        }
        return new ArrayList<>();
    }

    private List<UsuarioSistemaResponse> parseProfesionales(String json) {
        List<UsuarioSistemaResponse> resultados = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject root = reader.readObject();
            JsonArray data = root.getJsonArray("data");
            if (data == null) {
                return resultados;
            }
            for (var value : data) {
                JsonObject obj = value.asJsonObject();
                UsuarioSistemaResponse dto = new UsuarioSistemaResponse();
                dto.setTipo(UsuarioSistemaTipo.PROFESIONAL_SALUD);
                dto.setOrigen("PERIFERICO");

                String ciValue = obj.isNull("ci") ? null : obj.get("ci").toString();
                if (ciValue != null) {
                    dto.setId(ciValue);
                    dto.setNumeroDocumento(ciValue);
                }
                dto.setTipoDocumento(TipoDocumento.DO.name());

                String nombre = obj.getString("nombre", "");
                String apellidos = obj.getString("apellidos", "");
                dto.setPrimerNombre(nombre.isBlank() ? null : nombre);
                dto.setPrimerApellido(apellidos.isBlank() ? null : apellidos);
                dto.setNombreCompleto((nombre + " " + apellidos).trim());
                dto.setEmail(obj.containsKey("email") && !obj.isNull("email") ? obj.getString("email") : null);
                dto.setEspecialidad(obj.containsKey("especialidad") && !obj.isNull("especialidad")
                    ? obj.getString("especialidad") : null);
                resultados.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parseando profesionales desde periférico", e);
        }
        return resultados;
    }

    private List<UsuarioSistemaResponse> parseAdministradores(String json) {
        List<UsuarioSistemaResponse> resultados = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject root = reader.readObject();
            JsonArray data = root.getJsonArray("data");
            if (data == null) {
                return resultados;
            }
            for (var value : data) {
                JsonObject obj = value.asJsonObject();
                UsuarioSistemaResponse dto = new UsuarioSistemaResponse();
                dto.setTipo(UsuarioSistemaTipo.ADMINISTRADOR_CLINICA);
                dto.setOrigen("PERIFERICO");

                String idValue = obj.containsKey("id") && !obj.isNull("id") ? obj.getString("id") : null;
                if (idValue != null && !idValue.isBlank()) {
                    try {
                        UUID id = UUID.fromString(idValue);
                        dto.setId(id.toString());
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.FINE, "ID de administrador inválido", e);
                    }
                }

                dto.setUsername(obj.containsKey("username") && !obj.isNull("username") ? obj.getString("username") : null);
                String nombre = obj.getString("nombre", "");
                String apellidos = obj.getString("apellidos", "");
                dto.setPrimerNombre(nombre.isBlank() ? null : nombre);
                dto.setPrimerApellido(apellidos.isBlank() ? null : apellidos);
                dto.setNombreCompleto((nombre + " " + apellidos).trim());
                dto.setTenantId(obj.containsKey("tenantId") && !obj.isNull("tenantId") ? obj.getString("tenantId") : null);
                resultados.add(dto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parseando administradores desde periférico", e);
        }
        return resultados;
    }

    private String resolvePeripheralUrl() {
        String url = System.getenv(PERIPHERAL_ENV_VAR);
        if (url == null || url.isBlank()) {
            url = System.getProperty(PERIPHERAL_SYS_PROP);
        }
        if (url == null || url.isBlank()) {
            url = DEFAULT_PERIPHERAL_URL;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String buildSearchTerm(String nombre, String apellido) {
        StringBuilder builder = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) {
            builder.append(nombre.trim());
        }
        if (apellido != null && !apellido.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(apellido.trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private Optional<Integer> parseInteger(String value) {
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
