package com.hcen.periferico.config;

import com.hcen.periferico.service.AuthenticationService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.logging.Logger;

@Singleton
@Startup
public class DefaultClinicaAdminInitializer {

    private static final Logger LOGGER = Logger.getLogger(DefaultClinicaAdminInitializer.class.getName());

    private static final String ENV_ENABLE = "CLINICA_ADMIN_BOOTSTRAP"; // set to "true" to enable
    private static final String ENV_USER = "CLINICA_ADMIN_USER";
    private static final String ENV_PASS = "CLINICA_ADMIN_PASS";
    private static final String ENV_NOMBRE = "CLINICA_ADMIN_NOMBRE";
    private static final String ENV_APELLIDOS = "CLINICA_ADMIN_APELLIDOS";
    private static final String ENV_RUT = "CLINICA_ADMIN_RUT";

    @EJB
    private AuthenticationService authService;

    @PostConstruct
    public void init() {
        String enable = System.getenv(ENV_ENABLE);
        if (enable == null || !enable.equalsIgnoreCase("true")) {
            return; // disabled by default
        }

        String username = System.getenv(ENV_USER);
        String password = System.getenv(ENV_PASS);
        String nombre = System.getenv(ENV_NOMBRE);
        String apellidos = System.getenv(ENV_APELLIDOS);
        String clinicaRut = System.getenv(ENV_RUT);

        if (isBlank(username) || isBlank(password) || isBlank(nombre) || isBlank(apellidos) || isBlank(clinicaRut)) {
            LOGGER.warning("Clinica admin bootstrap enabled but required env vars are missing. " +
                    "Provide CLINICA_ADMIN_USER, CLINICA_ADMIN_PASS, CLINICA_ADMIN_NOMBRE, CLINICA_ADMIN_APELLIDOS, CLINICA_ADMIN_RUT");
            return;
        }

        try {
            if (authService.isValidPassword(password)) {
                    LOGGER.info("Default clinic admin created for RUT=" + clinicaRut + ", user=" + username);
            } else {
                LOGGER.warning("Provided CLINICA_ADMIN_PASS does not meet password policy. Skipping bootstrap.");
            }
        } catch (IllegalArgumentException e) {
            // likely already exists
            LOGGER.info("Clinic admin already exists for user=" + username + ", RUT=" + clinicaRut + "); skipping.");
        } catch (Exception ex) {
            LOGGER.severe("Failed to bootstrap clinic admin: " + ex.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

