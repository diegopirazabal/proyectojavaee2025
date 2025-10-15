package com.hcen.periferico.service;

import com.hcen.core.domain.administrador_clinica;
import com.hcen.periferico.dao.AdministradorClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

@Stateless
public class AuthenticationService {

    @EJB
    private AdministradorClinicaDAO adminDAO;

    /**
     * Autentica un administrador de clínica
     * @param username Username del administrador
     * @param password Contraseña en texto plano
     * @param clinicaRut RUT de la clínica (para multi-tenancy)
     * @return El administrador autenticado o null si falla la autenticación
     */
    public administrador_clinica authenticate(String username, String password, String clinicaRut) {
        System.out.println("[AUTH DEBUG] Intentando autenticar:");
        System.out.println("  Username: " + username);
        System.out.println("  Clinica RUT: " + clinicaRut);
        System.out.println("  Password length: " + (password != null ? password.length() : "null"));

        Optional<administrador_clinica> adminOpt = adminDAO.findByUsernameAndClinica(username, clinicaRut);

        System.out.println("[AUTH DEBUG] Usuario encontrado: " + adminOpt.isPresent());

        if (adminOpt.isPresent()) {
            administrador_clinica admin = adminOpt.get();
            System.out.println("[AUTH DEBUG] Usuario DB: " + admin.getUsername());
            System.out.println("[AUTH DEBUG] Clinica DB: " + admin.getClinica());
            System.out.println("[AUTH DEBUG] Password hash DB: " + admin.getPassword().substring(0, 20) + "...");

            boolean passwordMatch = verifyPassword(password, admin.getPassword());
            System.out.println("[AUTH DEBUG] Password match: " + passwordMatch);

            if (passwordMatch) {
                return admin;
            }
        }
        return null;
    }

    /**
     * Crea un nuevo administrador de clínica
     */
    public administrador_clinica createAdmin(String username, String password, String nombre,
                                           String apellidos, String clinicaRut) {
        // Verificar que no exista ya un administrador con ese username en esa clínica
        if (adminDAO.existsByUsernameAndClinica(username, clinicaRut)) {
            throw new IllegalArgumentException("Ya existe un administrador con ese username en esta clínica");
        }

        // Validar contraseña
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException(
                "La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas y números"
            );
        }

        String hashedPassword = hashPassword(password);
        administrador_clinica admin = new administrador_clinica(username, hashedPassword, nombre, apellidos, clinicaRut);

        return adminDAO.save(admin);
    }

    /**
     * Cambia la contraseña de un administrador
     */
    public boolean changePassword(administrador_clinica admin, String currentPassword, String newPassword) {
        if (verifyPassword(currentPassword, admin.getPassword())) {
            if (!isValidPassword(newPassword)) {
                throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas y números"
                );
            }
            admin.setPassword(hashPassword(newPassword));
            adminDAO.save(admin);
            return true;
        }
        return false;
    }

    /**
     * Resetea la contraseña de un administrador (sin verificar la anterior)
     */
    public void resetPassword(administrador_clinica admin, String newPassword) {
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException(
                "La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas y números"
            );
        }
        admin.setPassword(hashPassword(newPassword));
        adminDAO.save(admin);
    }

    /**
     * Hashea una contraseña usando BCrypt
     */
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Verifica una contraseña contra su hash
     */
    private boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valida que una contraseña cumpla con los requisitos mínimos
     */
    public boolean isValidPassword(String password) {
        return password != null &&
               password.length() >= 8 &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*[a-z].*") &&
               password.matches(".*[0-9].*");
    }
}
