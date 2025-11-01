package com.hcen.mocklaboratorio;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Minimal JAX-RS application to expose the mock endpoints under /api.
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    // No custom configuration required for the mock.
}
