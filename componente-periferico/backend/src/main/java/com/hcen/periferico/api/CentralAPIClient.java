package com.hcen.periferico.api;

import com.hcen.periferico.config.ClientCredentialsConfig;
import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.service.CentralAuthService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

/**
 * Cliente REST para comunicarse con el componente central (INUS)
 * Maneja todas las operaciones relacionadas con usuarios de salud
 */
@Stateless
public class CentralAPIClient {

    private static final Logger LOGGER = Logger.getLogger(CentralAPIClient.class.getName());

    // URL base del componente central - TODO: Hacer configurable via properties/env var
    private static final String CENTRAL_BASE_URL = System.getenv("HCEN_CENTRAL_URL") != null
        ? System.getenv("HCEN_CENTRAL_URL")
        : "http://localhost:8080";

    private static final String API_USUARIOS = CENTRAL_BASE_URL + "/api/usuarios";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    // Nuevos servicios para autenticación JWT (no tocar código existente)
    @EJB
    private ClientCredentialsConfig credentialsConfig;
    
    @EJB
    private CentralAuthService authService;

    private final HttpClient httpClient;

    public CentralAPIClient() {
        this.httpClient = createHttpClient();
    }
    
    /**
     * Crea un HttpClient que acepta certificados SSL no confiables
     * NOTA: Esto es solo para desarrollo. En producción debe usarse un truststore apropiado.
     */
    private HttpClient createHttpClient() {
        try {
            // TrustManager que acepta todos los certificados
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            // HostnameVerifier que acepta todos los hostnames
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            
            // Configurar SSLContext con el TrustManager que acepta todo
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // Configurar SSL parameters para deshabilitar endpoint identification
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("");
            
            LOGGER.warning("CentralAPIClient configurado con SSL bypass - SIN VALIDACIÓN DE CERTIFICADOS (solo para desarrollo)");
            
            return HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .sslContext(sslContext)
                .sslParameters(sslParams)
                .build();
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se pudo configurar SSL permisivo, usando cliente por defecto", e);
            return HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        }
    }

    /**
     * Verifica si un usuario existe en el componente central por cédula
     */
    public boolean verificarUsuarioExiste(String cedula) {
        try {
            String url = API_USUARIOS + "/verificar/" + cedula;
            LOGGER.info("Verificando existencia de usuario en central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonReader jsonReader = Json.createReader(new StringReader(response.body()));
                JsonObject jsonObject = jsonReader.readObject();
                boolean existe = jsonObject.getBoolean("existe", false);
                LOGGER.info("Usuario " + cedula + " existe en central: " + existe);
                return existe;
            } else {
                LOGGER.warning("Error al verificar usuario. Status: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar usuario en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Registra un usuario en el componente central.
     *
     * NOTA: A partir de la migración, el central NO almacena tenant_id.
     * Si tenantId es null, se registra como usuario único global.
     * Si tenantId no es null, se mantiene compatibilidad con versión anterior (temporal).
     */
    public usuario_salud_dto registrarUsuarioEnClinica(String cedula, TipoDocumento tipoDocumento,
                                                       String primerNombre, String segundoNombre,
                                                       String primerApellido, String segundoApellido,
                                                       String email, LocalDate fechaNacimiento,
                                                       String tenantId) {
        try {
            String url = API_USUARIOS + "/registrar";
            LOGGER.info("=== Registrando usuario en central ===");
            LOGGER.info("URL completa: " + url);
            LOGGER.info("CENTRAL_BASE_URL: " + CENTRAL_BASE_URL);
            LOGGER.info("tenantId: " + (tenantId != null ? tenantId : "null (usuario global)"));

            // Construir JSON del request
            var jsonBuilder = Json.createObjectBuilder()
                .add("cedula", cedula)
                .add("tipoDocumento", tipoDocumento.name())
                .add("primerNombre", primerNombre)
                .add("primerApellido", primerApellido)
                .add("email", email)
                .add("fechaNacimiento", fechaNacimiento.toString());

            // SOLO agregar tenantId si no es null (compatibilidad temporal)
            // Cuando el central migre, este campo será ignorado
            if (tenantId != null) {
                jsonBuilder.add("tenantId", tenantId);
            }

            // Agregar campos opcionales
            if (segundoNombre != null && !segundoNombre.isEmpty()) {
                jsonBuilder.add("segundoNombre", segundoNombre);
            }
            if (segundoApellido != null && !segundoApellido.isEmpty()) {
                jsonBuilder.add("segundoApellido", segundoApellido);
            }

            String jsonBody = jsonBuilder.build().toString();
            LOGGER.fine("Request body: " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Aceptar tanto 200 (ya existe) como 201 (creado) como éxito
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                LOGGER.info("Usuario registrado exitosamente en central (status: " + response.statusCode() + ")");
                return parseUsuarioFromJson(response.body());
            } else {
                String errorMsg = "Error al registrar usuario. Status: " + response.statusCode() +
                                ", Body: " + response.body();
                LOGGER.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene datos de un usuario por cédula desde el componente central
     */
    public usuario_salud_dto getUsuarioByCedula(String cedula) {
        try {
            String url = API_USUARIOS + "/" + cedula;
            LOGGER.info("Obteniendo usuario desde central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseUsuarioFromJson(response.body());
            } else if (response.statusCode() == 404) {
                LOGGER.info("Usuario no encontrado en central: " + cedula);
                return null;
            } else {
                LOGGER.warning("Error al obtener usuario. Status: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario desde central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos los usuarios de una clínica desde el componente central
     */
    public java.util.List<usuario_salud_dto> getAllUsuariosByTenantId(String tenantId) {
        try {
            String url = API_USUARIOS + "?tenantId=" + tenantId;
            LOGGER.info("Obteniendo todos los usuarios desde central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseUsuariosListFromJson(response.body());
            } else {
                LOGGER.warning("Error al obtener usuarios. Status: " + response.statusCode());
                return new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios desde central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Busca usuarios por nombre o apellido filtrados por tenant_id desde el componente central
     */
    public java.util.List<usuario_salud_dto> searchUsuariosByTenantId(String searchTerm, String tenantId) {
        try {
            String encodedTerm = java.net.URLEncoder.encode(searchTerm, java.nio.charset.StandardCharsets.UTF_8);
            String url = API_USUARIOS + "?tenantId=" + tenantId + "&search=" + encodedTerm;
            LOGGER.info("Buscando usuarios en central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseUsuariosListFromJson(response.body());
            } else {
                LOGGER.warning("Error al buscar usuarios. Status: " + response.statusCode());
                return new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar usuarios en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Desasocia un usuario de una clínica en el componente central
     */
    public boolean deleteUsuarioDeClinica(String cedula, String tenantId) {
        try {
            String url = API_USUARIOS + "/" + cedula + "/clinica/" + tenantId;
            LOGGER.info("Eliminando usuario de clínica en central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .DELETE()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("Usuario desasociado exitosamente de la clínica");
                return true;
            } else if (response.statusCode() == 404) {
                LOGGER.warning("No se encontró la asociación usuario-clínica");
                return false;
            } else {
                String errorMsg = "Error al desasociar usuario. Status: " + response.statusCode() +
                                ", Body: " + response.body();
                LOGGER.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error al desasociar usuario en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea la respuesta JSON del central a DTO
     */
    private usuario_salud_dto parseUsuarioFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            usuario_salud_dto dto = new usuario_salud_dto();
            dto.setCedula(jsonObject.getString("cedula"));
            dto.setPrimerNombre(jsonObject.getString("primerNombre"));
            dto.setSegundoNombre(jsonObject.getString("segundoNombre", null));
            dto.setPrimerApellido(jsonObject.getString("primerApellido"));
            dto.setSegundoApellido(jsonObject.getString("segundoApellido", null));
            dto.setEmail(jsonObject.getString("email"));

            String tipoDocStr = jsonObject.getString("tipoDocumento", "DO");
            dto.setTipoDocumento(TipoDocumento.valueOf(tipoDocStr));

            String fechaNacStr = jsonObject.getString("fechaNacimiento", null);
            if (fechaNacStr != null) {
                dto.setFechaNacimiento(LocalDate.parse(fechaNacStr));
            }

            return dto;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al parsear JSON de usuario", e);
            throw new RuntimeException("Error al procesar respuesta del componente central", e);
        }
    }

    /**
     * Parsea una lista de usuarios desde JSON
     */
    private java.util.List<usuario_salud_dto> parseUsuariosListFromJson(String jsonString) {
        java.util.List<usuario_salud_dto> usuarios = new java.util.ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonArray jsonArray = reader.readArray();

            for (JsonValue jsonValue : jsonArray) {
                JsonObject jsonObject = jsonValue.asJsonObject();

                usuario_salud_dto dto = new usuario_salud_dto();
                dto.setCedula(jsonObject.getString("cedula"));
                dto.setPrimerNombre(jsonObject.getString("primerNombre"));
                dto.setSegundoNombre(jsonObject.getString("segundoNombre", null));
                dto.setPrimerApellido(jsonObject.getString("primerApellido"));
                dto.setSegundoApellido(jsonObject.getString("segundoApellido", null));
                dto.setEmail(jsonObject.getString("email"));

                String tipoDocStr = jsonObject.getString("tipoDocumento", "DO");
                dto.setTipoDocumento(TipoDocumento.valueOf(tipoDocStr));

                String fechaNacStr = jsonObject.getString("fechaNacimiento", null);
                if (fechaNacStr != null) {
                    dto.setFechaNacimiento(LocalDate.parse(fechaNacStr));
                }

                usuarios.add(dto);
            }

            return usuarios;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al parsear JSON de lista de usuarios", e);
            throw new RuntimeException("Error al procesar respuesta del componente central", e);
        }
    }
    
    // ========== MÉTODOS AUXILIARES PARA JWT (NUEVA FUNCIONALIDAD) ==========
    
    /**
     * Crea un HttpRequest.Builder con JWT inyectado automáticamente
     * Usar este método para nuevas peticiones que requieran JWT
     */
    private HttpRequest.Builder createAuthenticatedRequestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(TIMEOUT);
        
        // Inyectar JWT si está disponible
        if (authService != null && authService.hasToken()) {
            String token = authService.getToken();
            builder.header("Authorization", "Bearer " + token);
        }
        
        return builder;
    }
    
    /**
     * Método auxiliar para hacer GET con JWT
     */
    private HttpResponse<String> executeAuthenticatedGet(String url) throws IOException, InterruptedException {
        HttpRequest request = createAuthenticatedRequestBuilder(url).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    /**
     * Método auxiliar para hacer POST con JWT
     */
    private HttpResponse<String> executeAuthenticatedPost(String url, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = createAuthenticatedRequestBuilder(url)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    /**
     * Método auxiliar para hacer DELETE con JWT
     */
    private HttpResponse<String> executeAuthenticatedDelete(String url) throws IOException, InterruptedException {
        HttpRequest request = createAuthenticatedRequestBuilder(url).DELETE().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
