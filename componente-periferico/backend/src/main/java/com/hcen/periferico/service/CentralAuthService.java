package com.hcen.periferico.service;

import com.hcen.periferico.config.ClientCredentialsConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;

/**
 * Servicio de autenticación con componente-central
 * Obtiene y mantiene JWT en memoria
 */
@Singleton
@Startup
public class CentralAuthService {
    
    private static final Logger LOGGER = Logger.getLogger(CentralAuthService.class.getName());
    
    @EJB
    private ClientCredentialsConfig credentialsConfig;
    
    @Inject
    private CloseableHttpClient httpClient;
    
    private String currentToken;
    private volatile boolean centralAuthDisabled;
    
    @PostConstruct
    public void init() {
        // Obtener token al inicio
        authenticate();
    }
    
    /**
     * Autentica con componente-central y obtiene JWT
     */
    public synchronized boolean authenticate() {
        if (centralAuthDisabled) {
            LOGGER.fine("Autenticación con componente-central deshabilitada tras fallos previos.");
            return false;
        }
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
                String responseBody = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity())
                    : "";
                
                if (statusCode == 200) {
                    // Parsear respuesta y extraer token
                    try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
                        JsonObject jsonResponse = jsonReader.readObject();
                        this.currentToken = jsonResponse.getString("accessToken");
                        
                        LOGGER.info("Autenticación exitosa, token obtenido");
                        return true;
                    }
                } else if (statusCode == 404 || statusCode == 405) {
                    centralAuthDisabled = true;
                    LOGGER.log(Level.WARNING,
                        "Endpoint de autenticación {0} no disponible (HTTP {1}). Se continuará sin token.",
                        new Object[]{credentialsConfig.getAuthTokenUrl(), statusCode});
                    return false;
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
        this.centralAuthDisabled = false;
        authenticate();
    }
    
    /**
     * Verifica si hay un token disponible
     */
    public boolean hasToken() {
        return currentToken != null && !currentToken.isEmpty();
    }
}
