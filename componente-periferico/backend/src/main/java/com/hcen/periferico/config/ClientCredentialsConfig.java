package com.hcen.periferico.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * 1. Archivo .env en el directorio raíz del proyecto (desarrollo local)
     * 2. Variables de entorno del sistema (HCEN_CLIENT_ID, HCEN_CLIENT_SECRET, HCEN_CENTRAL_URL)
     * 3. Archivo JSON en classpath (para desarrollo local - legacy)
     */
    private void loadCredentials() {
        // Intentar cargar .env primero (para desarrollo local)
        loadDotEnv();

        // Detectar ambiente
        String railwayDomain = System.getenv(ENV_RAILWAY_DOMAIN);
        boolean isProduction = railwayDomain != null && !railwayDomain.isBlank();
        this.activeEnvironment = isProduction ? "production" : "development";

        LOGGER.info("=" .repeat(60));
        LOGGER.info(isProduction
            ? "Ambiente detectado: PRODUCTION (Railway: " + railwayDomain + ")"
            : "Ambiente detectado: DEVELOPMENT (localhost)");

        // Intentar cargar desde variables (primero System properties del .env, luego env variables)
        String envClientId = getEnvOrProperty(ENV_CLIENT_ID);
        String envClientSecret = getEnvOrProperty(ENV_CLIENT_SECRET);
        String envCentralUrl = getEnvOrProperty(ENV_CENTRAL_URL);

        if (envClientId != null && envClientSecret != null && envCentralUrl != null) {
            // Configuración desde variables de entorno o .env
            this.clientId = envClientId;
            this.clientSecret = envClientSecret;
            this.centralServerUrl = envCentralUrl;

            LOGGER.info("Credenciales cargadas desde VARIABLES DE ENTORNO / .ENV");
            LOGGER.info("Cliente ID: " + clientId);
            LOGGER.info("URL servidor central: " + centralServerUrl);
            LOGGER.info("=" .repeat(60));
        } else {
            // Fallback a archivo JSON (desarrollo local - legacy)
            LOGGER.info("Variables de entorno no encontradas, usando archivo JSON de fallback...");
            loadFromJsonFile();
        }
    }

    /**
     * Obtiene una variable primero de System properties (cargadas del .env) y luego de variables de entorno
     */
    private String getEnvOrProperty(String key) {
        // Primero intentar System properties (del .env)
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        // Fallback a variables de entorno del sistema
        return System.getenv(key);
    }

    /**
     * Intenta cargar variables de entorno desde archivo .env
     * Busca el archivo .env en el directorio raíz del proyecto
     */
    private void loadDotEnv() {
        // Buscar .env en varios directorios posibles
        String userDir = System.getProperty("user.dir");
        String[] possiblePaths = {
            "D:/Proys/JavaEE/proyectojavaee2025/componente-periferico/.env",  // Ruta absoluta
            "../.env",  // componente-periferico/.env (relativa desde backend/)
            "../../.env", // desde backend/target/
            ".env",  // directorio actual
            userDir + "/../.env",  // desde user.dir
            userDir + "/../../componente-periferico/.env"
        };

        LOGGER.info("Buscando archivo .env... (user.dir=" + userDir + ")");

        for (String pathStr : possiblePaths) {
            try {
                Path envPath = Paths.get(pathStr).toAbsolutePath().normalize();
                if (Files.exists(envPath)) {
                    LOGGER.info("✓ Archivo .env encontrado: " + envPath);

                    try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
                        String line;
                        int varsLoaded = 0;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            // Ignorar comentarios y líneas vacías
                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }

                            // Parsear línea KEY=VALUE o KEY="VALUE"
                            int equalsIndex = line.indexOf('=');
                            if (equalsIndex > 0) {
                                String key = line.substring(0, equalsIndex).trim();
                                String value = line.substring(equalsIndex + 1).trim();

                                // Remover comillas si existen
                                if (value.startsWith("\"") && value.endsWith("\"")) {
                                    value = value.substring(1, value.length() - 1);
                                }

                                // Establecer como System property para que getEnvOrProperty() lo encuentre
                                System.setProperty(key, value);
                                varsLoaded++;

                                // Log solo las variables relevantes (no credenciales de BD)
                                if (key.startsWith("HCEN_")) {
                                    LOGGER.info("  → " + key + " = " +
                                        (key.contains("SECRET") ? "***" : value));
                                }
                            }
                        }

                        LOGGER.info("✓ Archivo .env cargado exitosamente (" + varsLoaded + " variables)");
                        return; // Salir después de cargar el primer .env encontrado
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "No se pudo cargar .env desde " + pathStr + ": " + e.getMessage());
            }
        }

        LOGGER.warning("⚠ No se encontró archivo .env en ninguna ubicación esperada");
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
