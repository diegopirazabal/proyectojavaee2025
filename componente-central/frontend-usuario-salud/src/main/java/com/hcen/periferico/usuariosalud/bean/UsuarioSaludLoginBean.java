package com.hcen.periferico.usuariosalud.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Named
@SessionScoped
public class UsuarioSaludLoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TEMP_USERNAME = "user";
    private static final String TEMP_PASSWORD = "user";
    private static final String DEFAULT_DOC_TYPE = "OTRO";
    private static final String DEFAULT_DOC_NUMBER = "85335898";

    private String username;
    private String password;
    private boolean loggedIn;

    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (credencialesValidas()) {
            loggedIn = true;
            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Bienvenido", "Consulta habilitada para usuario temporal"));
            return construirResultadoNavegacionDashboard();
        }

        loggedIn = false;
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Credenciales inválidas", "Usuario o contraseña incorrectos"));
        return null;
    }

    public String logout() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext external = context.getExternalContext();
        loggedIn = false;
        username = null;
        password = null;
        external.invalidateSession();
        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Sesión cerrada", "Has finalizado la consulta"));
        return "/login?faces-redirect=true";
    }

    public void checkAuthentication() throws IOException {
        // Verificar si hay cookie JWT (sesión OIDC) antes de redirigir
        if (!loggedIn && !hasOidcSession()) {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            external.getFlash().setRedirect(true); // evitar NPE de JSF al redirigir desde preRenderView
            external.redirect(external.getRequestContextPath() + "/login.xhtml");
            FacesContext.getCurrentInstance().responseComplete();
        } else if (!loggedIn && hasOidcSession()) {
            // Marcar como logueado si hay cookie JWT válida
            loggedIn = true;
        }
    }

    public void redirectIfLoggedIn() throws IOException {
        if (loggedIn) {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            external.getFlash().setRedirect(true); // garantizar que el flash conoce la redirección
            external.redirect(external.getRequestContextPath() + construirRutaDashboard());
            FacesContext.getCurrentInstance().responseComplete();
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private boolean credencialesValidas() {
        return TEMP_USERNAME.equals(username) && TEMP_PASSWORD.equals(password);
    }

    private String construirResultadoNavegacionDashboard() {
        return "dashboard?faces-redirect=true&docType=%s&docNumber=%s".formatted(
                codificar(DEFAULT_DOC_TYPE),
                codificar(DEFAULT_DOC_NUMBER)
        );
    }

    private String construirRutaDashboard() {
        return "/dashboard.xhtml?docType=%s&docNumber=%s".formatted(
                codificar(DEFAULT_DOC_TYPE),
                codificar(DEFAULT_DOC_NUMBER)
        );
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private boolean hasOidcSession() {
        try {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            
            // Primero verificar cookie JWT (más confiable entre WARs)
            HttpServletRequest request = (HttpServletRequest) external.getRequest();
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        return true;
                    }
                }
            }
            
            // Fallback: verificar userInfo en sesión HTTP (mismo WAR)
            HttpSession session = (HttpSession) external.getSession(false);
            return session != null && session.getAttribute("userInfo") != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getOidcLoginUrl() {
        // redirect_uri DEBE ser fija y estar registrada en gub.uy
        ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
        String serverName = external.getRequestServerName();
        
        // Determinar si es producción o desarrollo
        boolean isProduction = "hcen-uy.web.elasticloud.uy".equals(serverName);
        String redirectUri;
        String baseUrl;
        
        if (isProduction) {
            // Producción
            baseUrl = "https://hcen-uy.web.elasticloud.uy";
            redirectUri = "https://hcen-uy.web.elasticloud.uy/api/auth/callback";
        } else {
            // Desarrollo - backend en /hcen-central
            baseUrl = "http://localhost:8080/hcen-central";
            redirectUri = "http://localhost:8080/hcen-central/api/auth/callback";
        }
        
        return baseUrl + "/api/auth/login?redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&origin=usuario-salud";
    }
}
