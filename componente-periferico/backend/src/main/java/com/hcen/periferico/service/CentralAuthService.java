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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Servicio de autenticación con componente-central
 * Obtiene y mantiene JWT en memoria
 */
@Singleton
@Startup
public class CentralAuthService {
    
    private static final Logger LOGGER = Logger.getLogger(CentralAuthService.class.getName());
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    
    @EJB
    private ClientCredentialsConfig credentialsConfig;
    
    private String currentToken;
    private final CloseableHttpClient httpClient;
    
    public CentralAuthService() {
        this.httpClient = createHttpClient();
    }
    /**
     * Crea un HttpClient que acepta certificados SSL no confiables usando Apache HttpClient 5
     * NOTA: Esto es solo para desarrollo. En producción debe usarse un truststore apropiado.
     */
    private CloseableHttpClient createHttpClient() {
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
            
            // Configurar SSLContext con el TrustManager que acepta todo
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Crear SSLConnectionSocketFactory con NoopHostnameVerifier
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
            );
            
            // Crear connection manager con SSL custom
            HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
            
            LOGGER.warning("HttpClient (Apache) configurado con SSL bypass - SIN VALIDACIÓN DE CERTIFICADOS (solo para desarrollo)");
            
            return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se pudo configurar SSL permisivo", e);
            return HttpClients.createDefault();
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
            
            // Crear POST request
            HttpPost httpPost = new HttpPost(credentialsConfig.getAuthTokenUrl());
            httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            
            // Ejecutar request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    // Parsear respuesta y extraer token
                    try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
                        JsonObject jsonResponse = jsonReader.readObject();
                        this.currentToken = jsonResponse.getString("accessToken");
                        
                        LOGGER.info("Autenticación exitosa, token obtenido");
                        return true;
                    }
                } else {
                    LOGGER.severe("Error en autenticación. Status: " + statusCode + ", Body: " + responseBody);
                    return false;
                }
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
