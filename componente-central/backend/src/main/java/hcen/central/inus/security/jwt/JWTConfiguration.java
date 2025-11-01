package hcen.central.inus.security.jwt;

import java.time.Instant;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;


// TODO: Implementar configuración JWT
// - getSecretKey(): String (debe estar en variable de entorno en producción)
// - getAccessTokenExpiration(): long (ej: 15 minutos)
// - getRefreshTokenExpiration(): long (ej: 7 días)
// - getIssuer(): String
// - getAlgorithm(): SignatureAlgorithm

/**
 * Configuración JWT para tokens internos del sistema
 * (NO confundir con los tokens de gub.uy)
 * 
 * Gestiona:
 * - Secret key para firmar tokens propios
 * - Tiempos de expiración (access token, refresh token)
 * - Algoritmo de firma (HS256, RS256, etc.)
 * - Issuer del sistema (ej: "hcen.uy")
 * 
 * Lee de: jwt-config.properties o variables de entorno
 */
@Singleton
@Startup
public class JWTConfiguration {

    public static final Logger logger = Logger.getLogger(JWTConfiguration.class.getName());
    private static final String CONFIG_FILE = "/META-INF/oidc-config.properties";

    private String jwtIssuer;
    private String jwtSecret;
    private String jwtAlgorithm;
    private long jwtAccessTokenExpiration;
    private long jwtRefreshTokenExpiration;

    @PostConstruct
    public void init(){
        logger.info("Inicializando JWT Configuration...");
        loadConfiguration();
        logger.info("Configuración JWT cargada exitosamente");
    }

    private void loadConfiguration(){
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("No se encontró el archivo: " + CONFIG_FILE);
            }

            props.load(input);
            
            // Cargar propiedades JWT
            this.jwtIssuer = getRequiredProperty(props, "jwt.issuer");
            this.jwtSecret = getRequiredProperty(props, "jwt.secret");
            this.jwtAlgorithm = props.getProperty("jwt.algorithm", "HS256");
            this.jwtAccessTokenExpiration = Long.parseLong(
                    props.getProperty("jwt.access.token.expiration", "900000") // 15 minutos default
            );
            this.jwtRefreshTokenExpiration = Long.parseLong(
                    props.getProperty("jwt.refresh.token.expiration", "604800000") // 7 días default
            );
            
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar la configuración del JWT", e);
        }
    }

    private String getRequiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Propiedad requerida no encontrada: " + key);
        }
        return value.trim();
    }

    // Getters
    public String getJwtIssuer() { return jwtIssuer; }
    public String getJwtSecret() { return jwtSecret; }
    public String getJwtAlgorithm() { return jwtAlgorithm; }
    public long getJwtAccessTokenExpiration() { return jwtAccessTokenExpiration; }
    public long getJwtRefreshTokenExpiration() { return jwtRefreshTokenExpiration; }

}
