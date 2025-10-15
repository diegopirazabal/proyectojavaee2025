package com.hcen.periferico.service;

import com.hcen.core.domain.AdministradorClinica;
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
    public AdministradorClinica authenticate(String username, String password, String clinicaRut) {
        Optional<AdministradorClinica> adminOpt = adminDAO.findByUsernameAndClinica(username, clinicaRut);

        if (adminOpt.isPresent()) {
            AdministradorClinica admin = adminOpt.get();
            if (verifyPassword(password, admin.getPassword())) {
                return admin;
            }
        }
        return null;
    }

    /**
     * Crea un nuevo administrador de clínica
     */
    public AdministradorClinica createAdmin(String username, String password, String nombre,
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
        AdministradorClinica admin = new AdministradorClinica(username, hashedPassword, nombre, apellidos, clinicaRut);

        return adminDAO.save(admin);
    }

    /**
     * Cambia la contraseña de un administrador
     */
    public boolean changePassword(AdministradorClinica admin, String currentPassword, String newPassword) {
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
    public void resetPassword(AdministradorClinica admin, String newPassword) {
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
