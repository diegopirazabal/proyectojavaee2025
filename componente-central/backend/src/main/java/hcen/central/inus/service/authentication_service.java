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
    @EJB
    private admin_hcen_dao adminDAO;

    public admin_hcen authenticate(String username, String password) {
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
