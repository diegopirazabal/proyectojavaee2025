package hcen.central.inus.config;

import hcen.central.inus.dao.admin_hcen_dao;
import hcen.central.inus.entity.admin_hcen;
import hcen.central.inus.service.authentication_service;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ensures that the platform always has at least one HCEN administrator.
 * Creates the default admin user on startup when it does not exist.
 */
@Singleton
@Startup
public class DefaultAdminInitializer {

    private static final Logger LOGGER = Logger.getLogger(DefaultAdminInitializer.class.getName());

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String DEFAULT_FIRST_NAME = "Administrador";
    private static final String DEFAULT_LAST_NAME = "HCEN";
    private static final String DEFAULT_EMAIL = "admin@hcen.local";

    @EJB
    private admin_hcen_dao adminDao;

    @EJB
    private authentication_service authenticationService;

    @PostConstruct
    public void ensureDefaultAdmin() {
        try {
            if (adminDao.existsByUsername(DEFAULT_USERNAME)) {
                LOGGER.fine(() -> "Default HCEN admin already present.");
                return;
            }

            LOGGER.info(() -> "Creating default HCEN admin account.");
            admin_hcen admin = authenticationService.createAdmin(
                    DEFAULT_USERNAME,
                    DEFAULT_PASSWORD,
                    DEFAULT_FIRST_NAME,
                    DEFAULT_LAST_NAME,
                    DEFAULT_EMAIL
            );

            LOGGER.info(() -> "Default HCEN admin created with id=" + admin.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to provision default HCEN admin user.", e);
        }
    }
}
