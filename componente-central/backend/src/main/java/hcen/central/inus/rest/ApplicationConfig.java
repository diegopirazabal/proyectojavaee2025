package hcen.central.inus.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Configuración de JAX-RS Application
 * Define el path base para todos los endpoints REST
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    // No necesita métodos adicionales
    // Todos los recursos con @Path serán descubiertos automáticamente
}
