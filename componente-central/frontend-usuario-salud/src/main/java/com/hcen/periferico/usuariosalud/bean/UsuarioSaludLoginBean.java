package com.hcen.periferico.usuariosalud.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
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
    private static final String DEFAULT_DOC_TYPE = "DO";
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
        if (!loggedIn) {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            external.redirect(external.getRequestContextPath() + "/login.xhtml");
            FacesContext.getCurrentInstance().responseComplete();
        }
    }

    public void redirectIfLoggedIn() throws IOException {
        if (loggedIn) {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
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

    public String getOidcLoginUrl() {
        // Detectar URL base según el contexto (funciona en desarrollo y producción)
        ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
        String scheme = external.getRequestScheme();
        String serverName = external.getRequestServerName();
        int serverPort = external.getRequestServerPort();
        
        // Construir base URL
        StringBuilder baseUrl = new StringBuilder(scheme).append("://").append(serverName);
        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }
        
        String redirectUri = baseUrl + "/api/auth/callback";
        return baseUrl + "/api/auth/login?redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }
}
