package com.hcen.periferico.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración de credenciales del cliente para autenticación con componente-central
 * Carga las credenciales desde client_credentials.json
 */
@Singleton
@Startup
public class ClientCredentialsConfig {
    
    private static final Logger LOGGER = Logger.getLogger(ClientCredentialsConfig.class.getName());
    
    // Ruta del archivo de configuración - se puede sobreescribir con property del sistema
    private static final String DEFAULT_CONFIG_PATH = "/opt/wildfly/standalone/credentials/client_credentials.json";
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
     * Carga las credenciales desde el archivo JSON
     * Intenta cargar desde:
     * 1. System property: -Dclient.credentials.path=/ruta/archivo.json
     * 2. Archivo externo: /opt/wildfly/standalone/credentials/client_credentials.json
     * 3. Classpath: /client_credentials.json (para desarrollo)
     */
    private void loadCredentials() {
        String configPath = System.getProperty("client.credentials.path", DEFAULT_CONFIG_PATH);
        InputStream is = null;
        String loadedFrom = null;
        
        try {
            // Intento 1: Archivo externo (filesystem)
            Path externalFile = Paths.get(configPath);
            if (Files.exists(externalFile)) {
                is = new FileInputStream(externalFile.toFile());
                loadedFrom = configPath;
                LOGGER.info("Cargando credenciales desde filesystem: " + configPath);
            } 
            // Intento 2: Classpath (desarrollo)
            else {
                is = getClass().getResourceAsStream(CLASSPATH_CONFIG);
                if (is != null) {
                    loadedFrom = "classpath:" + CLASSPATH_CONFIG;
                    LOGGER.info("Cargando credenciales desde classpath: " + CLASSPATH_CONFIG);
                } else {
                    throw new RuntimeException("No se encontró el archivo de credenciales ni en " + configPath + " ni en classpath");
                }
            }
            
            // Parsear JSON
            try (JsonReader reader = Json.createReader(is)) {
                JsonObject config = reader.readObject();
                
                this.clientId = config.getString("client_id");
                this.clientSecret = config.getString("client_secret");
                this.activeEnvironment = config.getString("active_environment", "development");
                
                // Obtener URL según ambiente activo
                JsonObject environments = config.getJsonObject("environments");
                JsonObject activeEnv = environments.getJsonObject(activeEnvironment);
                this.centralServerUrl = activeEnv.getString("central_server_url");
                
                LOGGER.info("✅ Credenciales cargadas exitosamente desde: " + loadedFrom);
                LOGGER.info("Cliente ID: " + clientId);
                LOGGER.info("Ambiente activo: " + activeEnvironment);
                LOGGER.info("URL servidor central: " + centralServerUrl);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar credenciales desde " + configPath, e);
            throw new RuntimeException("No se pudieron cargar las credenciales del cliente", e);
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
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
