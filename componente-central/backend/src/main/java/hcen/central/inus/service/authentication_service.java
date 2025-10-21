package hcen.central.inus.service;

import hcen.central.inus.dao.admin_hcen_dao;
import hcen.central.inus.entity.admin_hcen;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class authentication_service {

    private static final Logger LOGGER = Logger.getLogger(authentication_service.class.getName());
    private static final String DEV_LOGIN_ENV = "HCEN_ENABLE_DEV_LOGIN";
    private static final String DEV_LOGIN_PROP = "hcen.enableDevLogin";
    private static final String DEV_USERNAME = "admin";
    private static final String DEV_PASSWORD = "admin";

    @EJB
    private admin_hcen_dao adminDAO;

    public admin_hcen authenticate(String username, String password) {
        if (isDevLoginEnabled() && DEV_USERNAME.equals(username) && DEV_PASSWORD.equals(password)) {
            LOGGER.warning("Dev login enabled: granting access to hardcoded admin user.");
            return buildDevAdmin();
        }

        try {
            Optional<admin_hcen> adminOpt = adminDAO.findByUsername(username);

            if (adminOpt.isPresent()) {
                admin_hcen admin = adminOpt.get();
                if (admin.getActive() && verifyPassword(password, admin.getPasswordHash())) {
                    adminDAO.updateLastLogin(admin.getId());
                    return admin;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database authentication failed.", e);
        }

        return null;
    }

    public admin_hcen createAdmin(String username, String password, String firstName,
                                  String lastName, String email) {

        if (adminDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (adminDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = hashPassword(password);
        admin_hcen admin = new admin_hcen(username, hashedPassword, firstName, lastName, email);

        return adminDAO.save(admin);
    }

    public boolean changePassword(Long adminId, String currentPassword, String newPassword) {
        Optional<admin_hcen> adminOpt = adminDAO.findById(adminId);

        if (adminOpt.isPresent()) {
            admin_hcen admin = adminOpt.get();
            if (verifyPassword(currentPassword, admin.getPasswordHash())) {
                admin.setPasswordHash(hashPassword(newPassword));
                adminDAO.save(admin);
                return true;
            }
        }
        return false;
    }

    public void resetPassword(Long adminId, String newPassword) {
        Optional<admin_hcen> adminOpt = adminDAO.findById(adminId);

        if (adminOpt.isPresent()) {
            admin_hcen admin = adminOpt.get();
            admin.setPasswordHash(hashPassword(newPassword));
            adminDAO.save(admin);
        }
    }

    private admin_hcen buildDevAdmin() {
        admin_hcen admin = new admin_hcen(DEV_USERNAME, hashPassword(DEV_PASSWORD),
                "Dev", "Admin", "dev-admin@hcen.local");
        admin.setId(-1L);
        admin.setLastLogin(LocalDateTime.now());
        return admin;
    }

    private boolean isDevLoginEnabled() {
        String envValue = System.getenv(DEV_LOGIN_ENV);
        if (envValue != null) {
            return Boolean.parseBoolean(envValue);
        }
        return Boolean.parseBoolean(System.getProperty(DEV_LOGIN_PROP, "false"));
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    public boolean isValidPassword(String password) {
        return password != null &&
                password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*");
    }
}
