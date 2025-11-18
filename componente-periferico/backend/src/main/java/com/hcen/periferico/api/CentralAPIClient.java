package com.hcen.periferico.api;

import com.hcen.periferico.config.ClientCredentialsConfig;
import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.service.CentralAuthService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLParameters;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

/**
 * Cliente REST para comunicarse con el componente central (INUS)
 * Maneja todas las operaciones relacionadas con usuarios de salud
 */
@Stateless
public class CentralAPIClient {

    private static final Logger LOGGER = Logger.getLogger(CentralAPIClient.class.getName());

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
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            // Configurar SSLContext con el TrustManager que acepta todo
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // Configurar SSL parameters para deshabilitar endpoint identification
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm(null);

            // Deshabilitar la verificación de hostname del HttpClient (solo para dev)
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            
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
     * Obtiene la URL base del componente central desde la configuración
     */
    private String getCentralBaseUrl() {
        return credentialsConfig.getCentralServerUrl();
    }

    /**
     * Construye la URL completa del API de usuarios
     */
    private String getApiUsuariosUrl() {
        return getCentralBaseUrl() + "/api/usuarios";
    }

    /**
     * Construye la URL completa del API de historia clínica
     */
    private String getApiHistoriaUrl() {
        return getCentralBaseUrl() + "/api/historia-clinica";
    }

    /**
     * Verifica si un usuario existe en el componente central por cédula
     */
    public boolean verificarUsuarioExiste(String cedula) {
        try {
            String url = getApiUsuariosUrl() + "/verificar/" + cedula;
            LOGGER.info("Verificando existencia de usuario en central: " + url);

            HttpResponse<String> response = executeAuthenticatedGet(url);

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
            String url = getApiUsuariosUrl() + "/registrar";
            LOGGER.info("=== Registrando usuario en central ===");
            LOGGER.info("URL completa: " + url);
            LOGGER.info("CENTRAL_BASE_URL: " + getCentralBaseUrl());
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

            HttpResponse<String> response = executeAuthenticatedPost(url, jsonBody);

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
     * Registra un documento en la historia clínica del componente central.
     * Sobrecarga que acepta UUID para tenantId (para usar desde adapters).
     * Usa REQUIRES_NEW para ejecutarse en una transacción separada.
     * Si falla, no afecta la transacción que guardó el documento localmente.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public UUID registrarDocumentoHistoriaClinica(String cedula, UUID tenantId, UUID documentoId) {
        return registrarDocumentoHistoriaClinica(tenantId.toString(), cedula, documentoId);
    }

    /**
     * Registra un documento en la historia clínica del componente central.
     * Usa REQUIRES_NEW para ejecutarse en una transacción separada.
     * Si falla, no afecta la transacción que guardó el documento localmente.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public UUID registrarDocumentoHistoriaClinica(String tenantId, String cedula, UUID documentoId) {
        try {
            var body = Json.createObjectBuilder()
                .add("tenantId", tenantId)
                .add("cedula", cedula)
                .add("documentoId", documentoId.toString())
                .build();

            HttpResponse<String> response = executeAuthenticatedPost(
                getApiHistoriaUrl() + "/documentos",
                body.toString()
            );

            int status = response.statusCode();
            if (status != 201 && status != 200) {
                LOGGER.severe("Error al registrar documento en historia clínica central. Status: " + status +
                    ", Body: " + response.body());
                throw new IOException("Error HTTP " + status + " al registrar documento en historia clínica");
            }

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();
                String historiaId = json.getString("historiaId", null);
                if (historiaId == null || historiaId.isBlank()) {
                    throw new IOException("Respuesta del componente central sin historiaId");
                }
                return UUID.fromString(historiaId);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error comunicándose con el central para registrar documento", e);
            throw new RuntimeException("No se pudo registrar el documento en el componente central: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El componente central devolvió un historiaId inválido", e);
        }
    }

    /**
     * Obtiene datos de un usuario por cédula desde el componente central
     */
    public usuario_salud_dto getUsuarioByCedula(String cedula) {
        try {
            String url = getApiUsuariosUrl() + "/" + cedula;
            LOGGER.info("Obteniendo usuario desde central: " + url);

            HttpResponse<String> response = executeAuthenticatedGet(url);

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
            String url = getApiUsuariosUrl() + "?tenantId=" + tenantId;
            LOGGER.info("Obteniendo todos los usuarios desde central: " + url);

            HttpResponse<String> response = executeAuthenticatedGet(url);

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
            String url = getApiUsuariosUrl() + "?tenantId=" + tenantId + "&search=" + encodedTerm;
            LOGGER.info("Buscando usuarios en central: " + url);

            HttpResponse<String> response = executeAuthenticatedGet(url);

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
            String url = getApiUsuariosUrl() + "/" + cedula + "/clinica/" + tenantId;
            LOGGER.info("Eliminando usuario de clínica en central: " + url);

            HttpResponse<String> response = executeAuthenticatedDelete(url);

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
            dto.setTenantId(jsonObject.getString("tenantId", null));

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
                dto.setTenantId(jsonObject.getString("tenantId", null));

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
            LOGGER.info("=== JWT DISPONIBLE - Inyectando en header Authorization ===");
            LOGGER.info("Token (primeros 20 chars): " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            builder.header("Authorization", "Bearer " + token);
        } else {
            LOGGER.warning("=== JWT NO DISPONIBLE - No se inyectará header Authorization ===");
            LOGGER.warning("authService: " + (authService != null ? "present" : "null"));
            LOGGER.warning("hasToken: " + (authService != null ? authService.hasToken() : "N/A"));
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

    // ========== MÉTODOS PARA VALIDACIÓN DE PERMISOS DE ACCESO A DOCUMENTOS ==========

    /**
     * Valida si un profesional tiene permiso para acceder a un documento clínico
     * Llama al endpoint POST /api/politicas-acceso/validar del componente central
     *
     * @param documentoId UUID del documento
     * @param ciProfesional CI del profesional que solicita acceso
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional (puede ser null)
     * @return true si tiene permiso, false en caso contrario
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean validarAccesoDocumento(UUID documentoId, Integer ciProfesional, UUID tenantId, String especialidad) {
        try {
            String url = getCentralBaseUrl() + "/api/politicas-acceso/validar";

            JsonObject body = Json.createObjectBuilder()
                .add("documentoId", documentoId.toString())
                .add("ciProfesional", ciProfesional)
                .add("tenantId", tenantId.toString())
                .add("especialidad", especialidad != null ? especialidad : "")
                .build();

            LOGGER.info(String.format("Validando acceso a documento %s para profesional CI=%d, tenant=%s",
                documentoId, ciProfesional, tenantId));

            HttpResponse<String> response = executeAuthenticatedPost(url, body.toString());

            if (response.statusCode() == 200) {
                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    JsonObject json = reader.readObject();
                    JsonObject data = json.getJsonObject("data");
                    boolean tienePermiso = data.getBoolean("tienePermiso", false);

                    LOGGER.info(String.format("Resultado validación: %s", tienePermiso ? "PERMITIDO" : "DENEGADO"));
                    return tienePermiso;
                }
            } else {
                LOGGER.warning(String.format("Error al validar acceso. Status code: %d", response.statusCode()));
                return false; // En caso de error, denegar acceso por seguridad
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar acceso a documento", e);
            return false; // En caso de error, denegar acceso por seguridad
        }
    }

    /**
     * Envía una notificación al paciente solicitando acceso a un documento
     * Llama al endpoint POST /api/notifications/solicitudes-acceso del componente central
     *
     * @param cedulaPaciente Cédula del paciente dueño del documento
     * @param documentoId UUID del documento
     * @param ciProfesional CI del profesional solicitante
     * @param nombreProfesional Nombre completo del profesional
     * @param especialidad Especialidad del profesional
     * @param tenantId UUID de la clínica
     * @param nombreClinica Nombre de la clínica
     * @param fechaDocumento Fecha del documento (para contexto)
     * @param motivoConsulta Motivo de consulta del documento
     * @param diagnostico Diagnóstico del documento
     * @return true si la notificación fue enviada exitosamente
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean solicitarAccesoDocumento(
            String cedulaPaciente,
            UUID documentoId,
            Integer ciProfesional,
            String nombreProfesional,
            String especialidad,
            UUID tenantId,
            String nombreClinica,
            String fechaDocumento,
            String motivoConsulta,
            String diagnostico) {
        try {
            String url = getCentralBaseUrl() + "/api/notifications/solicitudes-acceso";

            JsonObject body = Json.createObjectBuilder()
                .add("cedulaPaciente", cedulaPaciente)
                .add("documentoId", documentoId.toString())
                .add("profesionalCi", ciProfesional)
                .add("profesionalNombre", nombreProfesional)
                .add("especialidad", especialidad != null ? especialidad : "Sin especialidad")
                .add("tenantId", tenantId.toString())
                .add("nombreClinica", nombreClinica)
                .add("fechaDocumento", fechaDocumento)
                .add("motivoConsulta", motivoConsulta)
                .add("diagnostico", diagnostico != null ? diagnostico : "No especificado")
                .build();

            LOGGER.info(String.format("Solicitando acceso a documento %s para profesional %s (CI=%d) de %s",
                documentoId, nombreProfesional, ciProfesional, nombreClinica));

            HttpResponse<String> response = executeAuthenticatedPost(url, body.toString());
            int statusCode = response.statusCode();

            if (statusCode == 200 || statusCode == 201) {
                LOGGER.info("Notificación de solicitud de acceso enviada exitosamente");
                return true;
            } else if (isAuthStatus(statusCode)) {
                LOGGER.warning(String.format(
                    "Solicitud de acceso respondió con código %d (auth deshabilitada en dev). Ignorando.", statusCode));
                return true;
            } else {
                LOGGER.warning(String.format("Error al enviar solicitud de acceso. Status code: %d, Body: %s",
                    statusCode, response.body()));
                return false;
            }
        } catch (Exception e) {
            if (isSslException(e) || isAuthException(e)) {
                LOGGER.warning("Error SSL/JWT detectado, ignorando en entorno de desarrollo: " + e.getMessage());
                return true;
            } else {
                LOGGER.log(Level.SEVERE, "Error al solicitar acceso a documento", e);
                return false;
            }
        }
    }

    private boolean isAuthStatus(int status) {
        return status == 401 || status == 403;
    }

    private boolean isAuthException(Throwable throwable) {
        while (throwable != null) {
            String message = throwable.getMessage();
            if (message != null && (message.contains("JWT") || message.contains("jwt") || message.contains("Token"))) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private boolean isSslException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof SSLException) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }
}
