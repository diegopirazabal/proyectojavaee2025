package com.hcen.periferico.service;

import com.hcen.periferico.entity.profesional_salud;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class ProfesionalService {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    @EJB
    private ProfesionalSaludDAO profesionalDAO;

    /**
     * Crea o actualiza un profesional de salud
     */
    public profesional_salud saveProfesional(Integer ci, String nombre, String apellidos,
                                           UUID especialidadId, String email, String password, UUID tenantId) {
        // Validaciones
        if (ci == null || ci <= 0) {
            throw new IllegalArgumentException("La cédula es requerida y debe ser válida");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es requerido");
        }
        if (apellidos == null || apellidos.trim().isEmpty()) {
            throw new IllegalArgumentException("Los apellidos son requeridos");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }

        Optional<profesional_salud> existing = profesionalDAO.findByCiAndTenantId(ci, tenantId);
        profesional_salud profesional;

        if (existing.isPresent()) {
            // Actualizar existente
            profesional = existing.get();

            // Si se proporciona una nueva contraseña, hashearla (validación hecha en Resource)
            if (password != null && !password.trim().isEmpty()) {
                profesional.setPassword(hashPassword(password));
            }
            // Si password es null o vacío, no se cambia la contraseña actual
        } else {
            // Crear nuevo
            profesional = new profesional_salud();
            profesional.setCi(ci);
            profesional.setTenantId(tenantId);
            profesional.setActive(true); // Por defecto activo

            // Hashear contraseña (validación hecha en Resource)
            profesional.setPassword(hashPassword(password));
        }

        profesional.setNombre(nombre.trim());
        profesional.setApellidos(apellidos.trim());
        profesional.setEspecialidadId(especialidadId);
        profesional.setEmail(email.trim());

        // Para entidades existentes ya managed, los cambios se persisten automáticamente
        // Para nuevas entidades, necesitamos merge
        if (existing.isPresent()) {
            // Ya está managed, los cambios se persisten al final de la transacción
            return profesional;
        } else {
            // Nueva entidad, necesita merge
            return profesionalDAO.save(profesional);
        }
    }

    /**
     * Obtiene un profesional por su cédula y tenant
     */
    public Optional<profesional_salud> getProfesionalByCi(Integer ci, UUID tenantId) {
        return profesionalDAO.findByCiAndTenantId(ci, tenantId);
    }

    /**
     * Lista todos los profesionales
     */
    public List<profesional_salud> getAllProfesionales() {
        return profesionalDAO.findAll();
    }

    /**
     * Busca profesionales por especialidad
     */
    public List<profesional_salud> getProfesionalesByEspecialidadId(UUID especialidadId) {
        return profesionalDAO.findByEspecialidadId(especialidadId);
    }

    /**
     * Busca profesionales por nombre o apellido
     */
    public List<profesional_salud> searchProfesionales(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllProfesionales();
        }
        return profesionalDAO.findByNombreOrApellido(searchTerm.trim());
    }

    /**
     * Elimina un profesional (borrado lógico)
     */
    public void deleteProfesional(Integer ci, UUID tenantId) {
        profesionalDAO.softDelete(ci, tenantId);
    }

    /**
     * Verifica si existe un profesional con una cédula dada en un tenant
     */
    public boolean existsProfesional(Integer ci, UUID tenantId) {
        return profesionalDAO.existsByCiAndTenantId(ci, tenantId);
    }

    /**
     * Lista profesionales paginados
     */
    public List<profesional_salud> getProfesionalesPaginated(int page) {
        return getProfesionalesPaginated(page, PAGE_SIZE);
    }

    public List<profesional_salud> getProfesionalesPaginated(int page, Integer size) {
        int resolvedSize = normalizePageSize(size);
        return profesionalDAO.findAllPaginated(page, resolvedSize);
    }

    /**
     * Busca profesionales por nombre o apellido con paginación
     */
    public List<profesional_salud> searchProfesionalesPaginated(String searchTerm, int page) {
        return searchProfesionalesPaginated(searchTerm, page, PAGE_SIZE);
    }

    public List<profesional_salud> searchProfesionalesPaginated(String searchTerm, int page, Integer size) {
        int resolvedSize = normalizePageSize(size);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return profesionalDAO.findAllPaginated(page, resolvedSize);
        }
        return profesionalDAO.findByNombreOrApellidoPaginated(searchTerm.trim(), page, resolvedSize);
    }

    /**
     * Cuenta total de profesionales
     */
    public long countProfesionales() {
        return profesionalDAO.countAll();
    }

    /**
     * Cuenta profesionales que coinciden con el término de búsqueda
     */
    public long countProfesionalesBySearch(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return countProfesionales();
        }
        return profesionalDAO.countByNombreOrApellido(searchTerm.trim());
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Lista profesionales paginados filtrados por clínica (tenantId)
     */
    public List<profesional_salud> getProfesionalesByTenantIdPaginated(UUID tenantId, int page) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        return profesionalDAO.findByTenantIdPaginated(tenantId, page, PAGE_SIZE);
    }

    /**
     * Busca profesionales por nombre o apellido con paginación filtrados por clínica
     */
    public List<profesional_salud> searchProfesionalesByTenantIdPaginated(
            String searchTerm, UUID tenantId, int page) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getProfesionalesByTenantIdPaginated(tenantId, page);
        }
        return profesionalDAO.findByNombreOrApellidoAndTenantIdPaginated(
            searchTerm.trim(), tenantId, page, PAGE_SIZE);
    }

    /**
     * Cuenta total de profesionales de una clínica
     */
    public long countProfesionalesByTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        return profesionalDAO.countByTenantId(tenantId);
    }

    /**
     * Cuenta profesionales que coinciden con el término de búsqueda en una clínica
     */
    public long countProfesionalesBySearchAndTenantId(String searchTerm, UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return countProfesionalesByTenantId(tenantId);
        }
        return profesionalDAO.countByNombreOrApellidoAndTenantId(searchTerm.trim(), tenantId);
    }

    /**
     * Hashea una contraseña usando BCrypt
     */
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}
