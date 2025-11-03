package com.hcen.periferico.service;

import com.hcen.periferico.config.ClientCredentialsConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Servicio de autenticación con componente-central
 * Obtiene y mantiene JWT en memoria
 */
@Singleton
@Startup
public class CentralAuthService {
    
    private static final Logger LOGGER = Logger.getLogger(CentralAuthService.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    @EJB
    private ClientCredentialsConfig credentialsConfig;
    
    private String currentToken;
    private final HttpClient httpClient;
    
    public CentralAuthService() {
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
            
            LOGGER.warning("HttpClient configurado con SSL bypass - SIN VALIDACIÓN DE CERTIFICADOS (solo para desarrollo)");
            
            return HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .sslContext(sslContext)
                .sslParameters(sslParams)
                .build();
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se pudo configurar SSL permisivo, usando cliente por defecto", e);
            return HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        }
    }
    
    @PostConstruct
    public void init() {
        // Obtener token al inicio
        authenticate();
    }
    
    /**
     * Autentica con componente-central y obtiene JWT
     */
    public synchronized boolean authenticate() {
        try {
            LOGGER.info("Autenticando con componente-central...");
            
            // Construir request JSON
            String requestBody = Json.createObjectBuilder()
                .add("clientId", credentialsConfig.getClientId())
                .add("clientSecret", credentialsConfig.getClientSecret())
                .build()
                .toString();
            
            // Hacer request al endpoint de autenticación
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(credentialsConfig.getAuthTokenUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parsear respuesta y extraer token
                try (JsonReader jsonReader = Json.createReader(new StringReader(response.body()))) {
                    JsonObject jsonResponse = jsonReader.readObject();
                    this.currentToken = jsonResponse.getString("accessToken");
                    
                    LOGGER.info("Autenticación exitosa, token obtenido");
                    return true;
                }
            } else {
                LOGGER.severe("Error en autenticación. Status: " + response.statusCode() + ", Body: " + response.body());
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al autenticar con componente-central", e);
            return false;
        }
    }
    
    /**
     * Obtiene el token actual, si no existe intenta autenticar
     */
    public String getToken() {
        if (currentToken == null || currentToken.isEmpty()) {
            authenticate();
        }
        return currentToken;
    }
    
    /**
     * Invalida el token actual y obtiene uno nuevo
     */
    public synchronized void refreshToken() {
        LOGGER.info("Refrescando token...");
        this.currentToken = null;
        authenticate();
    }
    
    /**
     * Verifica si hay un token disponible
     */
    public boolean hasToken() {
        return currentToken != null && !currentToken.isEmpty();
    }
}
