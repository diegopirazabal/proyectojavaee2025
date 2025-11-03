package com.hcen.periferico.service;

import com.hcen.core.domain.profesional_salud;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.UUID;

@Stateless
public class ProfesionalAuthenticationService {

    @EJB
    private ProfesionalSaludDAO profesionalDAO;

    public profesional_salud authenticate(String email, String password, UUID tenantId) {
        Optional<profesional_salud> profOpt = profesionalDAO.findByEmailAndTenant(email, tenantId);
        if (profOpt.isPresent()) {
            profesional_salud prof = profOpt.get();
            if (verifyPassword(password, prof.getPassword())) {
                return prof;
            }
        }
        return null;
    }

    public boolean setCredential(Integer profesionalCi, String email, String rawPassword) {
        Optional<profesional_salud> profOpt = profesionalDAO.findByCi(profesionalCi);
        if (profOpt.isEmpty()) return false;
        profesional_salud prof = profOpt.get();
        // Se usa email para login; mantener username sin uso
        prof.setPassword(hashPassword(rawPassword));
        if (email != null && !email.isBlank()) {
            prof.setEmail(email);
        }
        profesionalDAO.save(prof);
        return true;
    }

    private String hashPassword(String password) { return BCrypt.hashpw(password, BCrypt.gensalt(12)); }
    private boolean verifyPassword(String password, String hashed) { try { return BCrypt.checkpw(password, hashed); } catch (Exception e) { return false; } }
}
