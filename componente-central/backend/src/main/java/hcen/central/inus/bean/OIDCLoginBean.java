package hcen.central.inus.bean;

import hcen.central.inus.dto.OIDCAuthRequest;
import hcen.central.inus.security.oidc.OIDCAuthenticationService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Managed bean para manejar el login OIDC con gub.uy
 */
@Named("oidcLoginBean")
@SessionScoped
public class OIDCLoginBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(OIDCLoginBean.class.getName());
    private static final long serialVersionUID = 1L;

    @Inject
    private OIDCAuthenticationService authService;

    private String redirectUri;

    @PostConstruct
    public void init() {
        // Construir redirect URI basado en el contexto de la aplicación
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        
        String scheme = externalContext.getRequestScheme();
        String serverName = externalContext.getRequestServerName();
        int serverPort = externalContext.getRequestServerPort();
        String contextPath = externalContext.getRequestContextPath();
        
        // Construir URL base
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        
        // Agregar puerto solo si no es el puerto por defecto
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }
        
        baseUrl.append(contextPath);
        
        // URL de callback
        redirectUri = baseUrl.toString() + "/api/auth/callback";
        
        LOGGER.info("Redirect URI configurada: " + redirectUri);
    }

    /**
     * Inicia el flujo de autenticación OIDC
     * Genera la URL de autorización y redirige al usuario a gub.uy
     */
    public void initiateLogin() {
        try {
            LOGGER.info("Iniciando flujo de login OIDC desde JSF");
            
            // Generar request de autorización
            OIDCAuthRequest authRequest = authService.initiateLogin(redirectUri);
            
            // Guardar datos en sesión
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            
            externalContext.getSessionMap().put("oidc_state", authRequest.getState());
            externalContext.getSessionMap().put("oidc_nonce", authRequest.getNonce());
            externalContext.getSessionMap().put("oidc_code_verifier", authRequest.getCodeVerifier());
            externalContext.getSessionMap().put("oidc_redirect_uri", redirectUri);
            
            LOGGER.info("Redirigiendo a: " + authRequest.getAuthorizationUrl());
            
            // Redirigir al authorization endpoint de gub.uy
            externalContext.redirect(authRequest.getAuthorizationUrl());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al redirigir a gub.uy", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error al iniciar autenticación", 
                    "No se pudo conectar con gub.uy. Por favor intente nuevamente."));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error iniciando login OIDC", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error de autenticación", 
                    "Ocurrió un error inesperado. Por favor intente nuevamente."));
        }
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
