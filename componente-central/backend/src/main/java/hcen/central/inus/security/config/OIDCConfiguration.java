package hcen.central.inus.security.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;


@Singleton
@Startup
public class OIDCConfiguration {

    private static final Logger logger = Logger.getLogger(OIDCConfiguration.class.getName());
    private static final String CONFIG_FILE = "/META-INF/oidc-config.properties";

    private String issuer;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String jwksUri;
    private String scope;

    // Configuración JWT propia (para tokens de sesión nuestros)
    private String jwtSecret;
    private long jwtExpirationMs;
    private long refreshTokenExpirationMs;

    @PostConstruct
    public void init() {
        logger.info("Inicializando configuración OIDC...");
        loadConfiguration();
        validateConfiguration();
        logger.info("Configuración OIDC cargada exitosamente");
    }

    private void loadConfiguration() {
        Properties props = new Properties();

        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("No se encontró el archivo: " + CONFIG_FILE);
            }

            props.load(input);

            // OIDC Properties
            this.issuer = getRequiredProperty(props, "oidc.issuer");
            this.clientId = getRequiredProperty(props, "oidc.client.id");
            this.clientSecret = getRequiredProperty(props, "oidc.client.secret");
            this.redirectUri = getRequiredProperty(props, "oidc.redirect.uri");
            this.authorizationEndpoint = getRequiredProperty(props, "oidc.authorization.endpoint");
            this.tokenEndpoint = getRequiredProperty(props, "oidc.token.endpoint");
            this.userInfoEndpoint = getRequiredProperty(props, "oidc.userinfo.endpoint");
            this.jwksUri = getRequiredProperty(props, "oidc.jwks.uri");
            this.scope = props.getProperty("oidc.scope", "openid profile email document");

            // JWT Properties
            this.jwtSecret = getRequiredProperty(props, "jwt.secret");
            this.jwtExpirationMs = Long.parseLong(
                    props.getProperty("jwt.access.token.expiration", "3600000") // 1 hora default
            );
            this.refreshTokenExpirationMs = Long.parseLong(
                    props.getProperty("jwt.refresh.token.expiration", "86400000") // 24 horas default
            );

        } catch (IOException e) {
            throw new RuntimeException("Error al cargar configuración OIDC", e);
        }
    }

    private String getRequiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Propiedad requerida no encontrada: " + key);
        }
        return value.trim();
    }

    private void validateConfiguration() {
        logger.info("Validando configuración OIDC...");
        
        // Validar URLs
        validateUrl(issuer, "issuer");
        validateUrl(authorizationEndpoint, "authorization endpoint");
        validateUrl(tokenEndpoint, "token endpoint");
        validateUrl(userInfoEndpoint, "userinfo endpoint");
        validateUrl(jwksUri, "JWKS URI");
        validateUrl(redirectUri, "redirect URI");
        
        logger.info("Configuración OIDC válida");
    }
    
    private void validateUrl(String url, String fieldName) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new RuntimeException(fieldName + " debe ser una URL válida (http:// o https://): " + url);
        }
    }

    // Getters
    public String getIssuer() { return issuer; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }
    public String getAuthorizationEndpoint() { return authorizationEndpoint; }
    public String getTokenEndpoint() { return tokenEndpoint; }
    public String getUserInfoEndpoint() { return userInfoEndpoint; }
    public String getUserinfoEndpoint() { return userInfoEndpoint; } // Alias
    public String getJwksUri() { return jwksUri; }
    public String getScope() { return scope; }
    public String getJwtSecret() { return jwtSecret; }
    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public long getRefreshTokenExpirationMs() { return refreshTokenExpirationMs; }
}