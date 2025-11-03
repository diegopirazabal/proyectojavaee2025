package com.hcen.periferico.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración de credenciales del cliente para autenticación con componente-central
 * Usa variables de entorno para configuración (Railway-friendly)
 * Fallback a client_credentials.json para desarrollo local
 */
@Singleton
@Startup
public class ClientCredentialsConfig {
    
    private static final Logger LOGGER = Logger.getLogger(ClientCredentialsConfig.class.getName());
    
    // Configuración mediante variables de entorno
    private static final String ENV_CLIENT_ID = "HCEN_CLIENT_ID";
    private static final String ENV_CLIENT_SECRET = "HCEN_CLIENT_SECRET";
    private static final String ENV_CENTRAL_URL = "HCEN_CENTRAL_URL";
    private static final String ENV_RAILWAY_DOMAIN = "RAILWAY_PUBLIC_DOMAIN";
    
    // Fallback para desarrollo local
    private static final String CLASSPATH_CONFIG = "/client_credentials.json";
    
    private String clientId;
    private String clientSecret;
    private String centralServerUrl;
    private String activeEnvironment;
    
    @PostConstruct
    public void init() {
        loadCredentials();
    }
    
    /**
     * Carga las credenciales desde variables de entorno o archivo JSON de fallback
     * Prioridad:
     * 1. Variables de entorno (HCEN_CLIENT_ID, HCEN_CLIENT_SECRET, HCEN_CENTRAL_URL)
     * 2. Archivo JSON en classpath (para desarrollo local)
     */
    private void loadCredentials() {
        // Detectar ambiente primero
        String railwayDomain = System.getenv(ENV_RAILWAY_DOMAIN);
        boolean isProduction = railwayDomain != null && !railwayDomain.isBlank();
        this.activeEnvironment = isProduction ? "production" : "development";
        
        LOGGER.info("=" .repeat(60));
        LOGGER.info(isProduction 
            ? "Ambiente detectado: PRODUCTION (Railway: " + railwayDomain + ")"
            : "Ambiente detectado: DEVELOPMENT (localhost)");
        
        // Intentar cargar desde variables de entorno primero
        String envClientId = System.getenv(ENV_CLIENT_ID);
        String envClientSecret = System.getenv(ENV_CLIENT_SECRET);
        String envCentralUrl = System.getenv(ENV_CENTRAL_URL);
        
        if (envClientId != null && envClientSecret != null && envCentralUrl != null) {
            // Configuración desde variables de entorno (Railway)
            this.clientId = envClientId;
            this.clientSecret = envClientSecret;
            this.centralServerUrl = envCentralUrl;
            
            LOGGER.info("Credenciales cargadas desde VARIABLES DE ENTORNO");
            LOGGER.info("Cliente ID: " + clientId);
            LOGGER.info("URL servidor central: " + centralServerUrl);
            LOGGER.info("=" .repeat(60));
        } else {
            // Fallback a archivo JSON (desarrollo local)
            LOGGER.info("Variables de entorno no encontradas, usando archivo JSON de fallback...");
            loadFromJsonFile();
        }
    }
    
    /**
     * Carga credenciales desde client_credentials.json (fallback para desarrollo)
     */
    private void loadFromJsonFile() {
        try (InputStream is = getClass().getResourceAsStream(CLASSPATH_CONFIG)) {
            if (is == null) {
                loadDevelopmentDefaults("No se encontró " + CLASSPATH_CONFIG + " en classpath");
                return;
            }
            
            try (JsonReader reader = Json.createReader(is)) {
                JsonObject config = reader.readObject();
                
                this.clientId = config.getString("client_id");
                this.clientSecret = config.getString("client_secret");
                
                // Obtener URL según ambiente detectado
                JsonObject environments = config.getJsonObject("environments");
                JsonObject activeEnv = environments.getJsonObject(activeEnvironment);
                this.centralServerUrl = activeEnv.getString("central_server_url");
                
                LOGGER.info("Credenciales cargadas desde: classpath:" + CLASSPATH_CONFIG);
                LOGGER.info("Cliente ID: " + clientId);
                LOGGER.info("Ambiente activo: " + activeEnvironment);
                LOGGER.info("URL servidor central: " + centralServerUrl);
                LOGGER.info("=" .repeat(60));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar credenciales desde JSON", e);
            loadDevelopmentDefaults("Error al cargar credenciales desde JSON: " + e.getMessage());
        }
    }
    
    private void loadDevelopmentDefaults(String reason) {
        if (activeEnvironment == null) {
            activeEnvironment = "development";
        }
        
        LOGGER.log(Level.WARNING, "Fallo al resolver credenciales dinámicas ({0}). Usando valores por defecto de desarrollo.", reason);
        this.clientId = "usuario-salud-local";
        this.clientSecret = "usuario-salud-local-secret";
        this.centralServerUrl = "http://localhost:8080/hcen-central";
        LOGGER.info("Cliente ID (dev): " + clientId);
        LOGGER.info("URL servidor central (dev): " + centralServerUrl);
        LOGGER.info("=" .repeat(60));
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String getCentralServerUrl() {
        return centralServerUrl;
    }
    
    public String getActiveEnvironment() {
        return activeEnvironment;
    }
    
    /**
     * Obtiene la URL completa del API de autenticación
     */
    public String getAuthTokenUrl() {
        return centralServerUrl + "/api/auth/token";
    }
    
    /**
     * Obtiene la URL base del API central
     */
    public String getApiBaseUrl() {
        return centralServerUrl + "/api";
    }
}
