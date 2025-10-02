package hcen.central.inus.service;

import hcen.central.inus.dao.AdminHCENDAO;
import hcen.central.inus.entity.AdminHCEN;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

@Stateless
public class AuthenticationService {
    
    @EJB
    private AdminHCENDAO adminDAO;
    
    public AdminHCEN authenticate(String username, String password) {
        Optional<AdminHCEN> adminOpt = adminDAO.findByUsername(username);
        
        if (adminOpt.isPresent()) {
            AdminHCEN admin = adminOpt.get();
            if (admin.getActive() && verifyPassword(password, admin.getPasswordHash())) {
                adminDAO.updateLastLogin(admin.getId());
                return admin;
            }
        }
        return null;
    }
    
    public AdminHCEN createAdmin(String username, String password, String firstName, 
                                String lastName, String email) {
        
        if (adminDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (adminDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        String hashedPassword = hashPassword(password);
        AdminHCEN admin = new AdminHCEN(username, hashedPassword, firstName, lastName, email);
        
        return adminDAO.save(admin);
    }
    
    public boolean changePassword(Long adminId, String currentPassword, String newPassword) {
        Optional<AdminHCEN> adminOpt = adminDAO.findById(adminId);
        
        if (adminOpt.isPresent()) {
            AdminHCEN admin = adminOpt.get();
            if (verifyPassword(currentPassword, admin.getPasswordHash())) {
                admin.setPasswordHash(hashPassword(newPassword));
                adminDAO.save(admin);
                return true;
            }
        }
        return false;
    }
    
    public void resetPassword(Long adminId, String newPassword) {
        Optional<AdminHCEN> adminOpt = adminDAO.findById(adminId);
        
        if (adminOpt.isPresent()) {
            AdminHCEN admin = adminOpt.get();
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