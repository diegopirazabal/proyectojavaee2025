package com.hcen.periferico.service;

import com.hcen.core.domain.profesional_salud;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.Optional;

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
                                           String especialidad, String email) {
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

        Optional<profesional_salud> existing = profesionalDAO.findByCi(ci);
        profesional_salud profesional;

        if (existing.isPresent()) {
            // Actualizar existente
            profesional = existing.get();
        } else {
            // Crear nuevo
            profesional = new profesional_salud();
            profesional.setCi(ci);
        }

        profesional.setNombre(nombre.trim());
        profesional.setApellidos(apellidos.trim());
        profesional.setEspecialidad(especialidad != null ? especialidad.trim() : null);
        profesional.setEmail(email != null ? email.trim() : null);

        return profesionalDAO.save(profesional);
    }

    /**
     * Obtiene un profesional por su cédula
     */
    public Optional<profesional_salud> getProfesionalByCi(Integer ci) {
        return profesionalDAO.findByCi(ci);
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
    public List<profesional_salud> getProfesionalesByEspecialidad(String especialidad) {
        return profesionalDAO.findByEspecialidad(especialidad);
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
     * Elimina un profesional
     */
    public void deleteProfesional(Integer ci) {
        profesionalDAO.deleteByCi(ci);
    }

    /**
     * Verifica si existe un profesional con una cédula dada
     */
    public boolean existsProfesional(Integer ci) {
        return profesionalDAO.existsByCi(ci);
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
}
