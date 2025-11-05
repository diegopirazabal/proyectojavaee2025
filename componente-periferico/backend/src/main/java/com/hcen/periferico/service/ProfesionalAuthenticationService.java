package com.hcen.periferico.service;

import com.hcen.periferico.entity.profesional_salud;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Logger;

import java.util.Optional;
import java.util.UUID;

@Stateless
public class ProfesionalAuthenticationService {

    @EJB
    private ProfesionalSaludDAO profesionalDAO;

    private static final Logger LOGGER = Logger.getLogger(ProfesionalAuthenticationService.class.getName());

    public profesional_salud authenticate(String email, String password, UUID tenantId) {
        LOGGER.info(() -> "[ProfesionalService] authenticate email=" + email + " tenant=" + tenantId);
        Optional<profesional_salud> profOpt = profesionalDAO.findByEmailAndTenant(email, tenantId);
        if (profOpt.isEmpty()) {
            LOGGER.warning("[ProfesionalService] profesional no encontrado para email/tenant");
            return null;
        }
        profesional_salud prof = profOpt.get();
        String stored = prof.getPassword();
        boolean ok = verifyPassword(password, stored);
        // Fallback dev: si no parece hash BCrypt, comparar plano (para datos legacy)
        if (!ok && stored != null && !stored.startsWith("$2")) {
            ok = password != null && password.equals(stored);
        }
        if (!ok) {
            LOGGER.warning("[ProfesionalService] password no coincide para ci=" + prof.getCi());
            return null;
        }
        LOGGER.info(() -> "[ProfesionalService] autenticado ci=" + prof.getCi());
        return prof;
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
