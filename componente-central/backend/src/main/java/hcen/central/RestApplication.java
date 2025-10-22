package hcen.central;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application Configuration
 * Escanea automáticamente todos los recursos REST en el classpath
 * que tengan las anotaciones @Path
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    // Al no sobrescribir getClasses(), JAX-RS escanea automáticamente
    // todos los recursos REST del WAR
}
