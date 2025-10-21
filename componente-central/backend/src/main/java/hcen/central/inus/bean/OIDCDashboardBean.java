package hcen.central.inus.bean;

import hcen.central.inus.dto.JWTTokenResponse;
import hcen.central.inus.dto.OIDCUserInfo;
import hcen.central.inus.security.jwt.JWTTokenProvider;
import hcen.central.inus.security.oidc.OIDCAuthenticationService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Managed bean para el dashboard que muestra información del usuario autenticado
 */
@Named("oidcDashboardBean")
@SessionScoped
public class OIDCDashboardBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(OIDCDashboardBean.class.getName());
    private static final long serialVersionUID = 1L;

    @Inject
    private JWTTokenProvider jwtTokenProvider;

    @Inject
    private OIDCAuthenticationService authService;

    // Datos de sesión
    private OIDCUserInfo userInfo;
    private JWTTokenResponse jwtToken;
    private Claims idTokenClaims;
    private String rawIdToken;
    private String sessionId;
    private Date sessionCreated;

    @PostConstruct
    public void init() {
        loadSessionData();
    }

    /**
     * Carga los datos de la sesión desde los atributos guardados
     */
    private void loadSessionData() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            
            // Recuperar datos guardados en sesión
            userInfo = (OIDCUserInfo) facesContext.getExternalContext().getSessionMap().get("userInfo");
            jwtToken = (JWTTokenResponse) facesContext.getExternalContext().getSessionMap().get("jwtToken");
            rawIdToken = (String) facesContext.getExternalContext().getSessionMap().get("rawIdToken");
            sessionId = (String) facesContext.getExternalContext().getSessionMap().get("sessionId");
            sessionCreated = (Date) facesContext.getExternalContext().getSessionMap().get("sessionCreated");
            
            // Parsear ID token si está disponible
            if (rawIdToken != null && !rawIdToken.isEmpty()) {
                try {
                    idTokenClaims = jwtTokenProvider.validateAccessToken(rawIdToken);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "No se pudo parsear ID token", e);
                }
            }
            
            // Si no hay datos de sesión, redirigir al login
            if (userInfo == null || jwtToken == null) {
                LOGGER.warning("No hay datos de sesión, redirigiendo a login");
                facesContext.getExternalContext().redirect("login.xhtml");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando datos de sesión", e);
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    public String logout() {
        try {
            if (userInfo != null) {
                authService.logout(userInfo.getSub());
            }
            
            // Limpiar sesión
            FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
            
            // Redirigir al login
            return "login.xhtml?faces-redirect=true";
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en logout", e);
            return null;
        }
    }

    // Métodos auxiliares para formateo

    public String getTruncatedAccessToken() {
        if (jwtToken != null && jwtToken.getAccessToken() != null) {
            String token = jwtToken.getAccessToken();
            return token.length() > 100 ? token.substring(0, 100) : token;
        }
        return "";
    }

    public String getTruncatedIdToken() {
        if (rawIdToken != null) {
            return rawIdToken.length() > 100 ? rawIdToken.substring(0, 100) : rawIdToken;
        }
        return "";
    }

    public String getFormattedIssuedAt() {
        if (idTokenClaims != null && idTokenClaims.getIssuedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return sdf.format(idTokenClaims.getIssuedAt());
        }
        return "N/A";
    }

    public String getFormattedExpiration() {
        if (idTokenClaims != null && idTokenClaims.getExpiration() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return sdf.format(idTokenClaims.getExpiration());
        }
        return "N/A";
    }

    public String getFormattedSessionCreated() {
        if (sessionCreated != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return sdf.format(sessionCreated);
        }
        return "N/A";
    }

    // Getters

    public OIDCUserInfo getUserInfo() {
        return userInfo;
    }

    public JWTTokenResponse getJwtToken() {
        return jwtToken;
    }

    public Claims getIdTokenClaims() {
        return idTokenClaims;
    }

    public String getRawIdToken() {
        return rawIdToken;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Date getSessionCreated() {
        return sessionCreated;
    }
}
