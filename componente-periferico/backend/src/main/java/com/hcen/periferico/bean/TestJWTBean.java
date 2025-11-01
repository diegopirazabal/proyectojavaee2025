package com.hcen.periferico.bean;

import com.hcen.periferico.config.ClientCredentialsConfig;
import com.hcen.periferico.service.CentralAuthService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Named("testJWTBean")
@ViewScoped
public class TestJWTBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @EJB
    private CentralAuthService authService;
    
    @EJB
    private ClientCredentialsConfig credentialsConfig;
    
    private String token;
    private Long expiresIn;
    private String mensaje;
    private String respuestaServicio;
    private String centralUrl;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    
    @PostConstruct
    public void init() {
        this.centralUrl = credentialsConfig != null ? credentialsConfig.getCentralServerUrl() : "N/A";
    }
    
    /**
     * Obtiene un nuevo token JWT desde componente-central
     */
    public void obtenerToken() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            
            // Refrescar token (esto llama al servicio de autenticación)
            authService.refreshToken();
            
            // Obtener el token recién generado
            this.token = authService.getToken();
            this.expiresIn = authService != null ? 900L : 0L; // 15 minutos por defecto
            
            if (token != null && !token.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_INFO, "Éxito", 
                    "Token JWT obtenido correctamente desde componente-central");
                this.respuestaServicio = null; // Limpiar respuesta anterior
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "No se pudo obtener el token JWT");
            }
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "Error al obtener token: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Consume un servicio protegido del componente-central usando el JWT
     */
    public void consumirServicio() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            
            if (token == null || token.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_WARN, "Advertencia", 
                    "Primero debe obtener un token JWT");
                return;
            }
            
            // Llamar al endpoint de usuarios-salud
            String url = credentialsConfig.getApiBaseUrl() + "/usuarios-salud";
            
            // Hacer petición con JWT en el header
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                this.respuestaServicio = "✅ Status: 200 OK\n\n" + 
                    "Respuesta del servidor:\n" + 
                    formatJson(response.body());
                addMessage(FacesMessage.SEVERITY_INFO, "Éxito", 
                    "Servicio consumido correctamente con JWT");
            } else if (response.statusCode() == 401) {
                this.respuestaServicio = "❌ Status: 401 Unauthorized\n\n" + 
                    "El token JWT fue rechazado:\n" + response.body();
                addMessage(FacesMessage.SEVERITY_ERROR, "Error 401", 
                    "Token expirado o inválido");
            } else {
                this.respuestaServicio = "⚠️ Status: " + response.statusCode() + "\n\n" + 
                    response.body();
                addMessage(FacesMessage.SEVERITY_WARN, "Respuesta inesperada", 
                    "Status code: " + response.statusCode());
            }
            
        } catch (Exception e) {
            this.respuestaServicio = "💥 Error de conexión:\n" + e.getMessage();
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "Error al consumir servicio: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Formatea JSON para mejor visualización
     */
    private String formatJson(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return "[] (Lista vacía - no hay usuarios registrados)";
        }
        // Intento simple de formateo
        return json.replace(",", ",\n  ")
                  .replace("{", "{\n  ")
                  .replace("}", "\n}")
                  .replace("[", "[\n  ")
                  .replace("]", "\n]");
    }
    
    /**
     * Helper para agregar mensajes JSF
     */
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
        this.mensaje = detail;
    }
    
    // Getters y Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    public String getRespuestaServicio() {
        return respuestaServicio;
    }
    
    public void setRespuestaServicio(String respuestaServicio) {
        this.respuestaServicio = respuestaServicio;
    }
    
    public String getCentralUrl() {
        return centralUrl;
    }
    
    public void setCentralUrl(String centralUrl) {
        this.centralUrl = centralUrl;
    }
}
