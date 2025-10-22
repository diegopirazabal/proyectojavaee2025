package com.hcen.periferico.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/multitenant-api")
public class RestApplication extends Application {
    // Esta clase configura el path base para todos los endpoints REST
    // No necesita implementación adicional, JAX-RS auto-descubre los recursos
}