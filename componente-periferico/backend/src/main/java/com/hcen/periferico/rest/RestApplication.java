package com.hcen.periferico.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class RestApplication extends Application {
    // Esta clase configura el path base para todos los endpoints REST
    // El ApplicationPath es "/" porque el WAR ya se despliega en /multitenant-api
    // Entonces los recursos REST estarán en: http://localhost:8080/multitenant-api/auth/login
    // No necesita implementación adicional, JAX-RS auto-descubre los recursos
}