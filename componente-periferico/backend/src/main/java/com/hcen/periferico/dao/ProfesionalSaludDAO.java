package com.hcen.periferico.dao;

import com.hcen.periferico.entity.profesional_salud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class ProfesionalSaludDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public profesional_salud save(profesional_salud profesional) {
        return em.merge(profesional);
    }

    public Optional<profesional_salud> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        profesional_salud profesional = em.find(profesional_salud.class, id);
        return Optional.ofNullable(profesional);
    }

    public Optional<profesional_salud> findByCiAndTenantId(Integer ci, UUID tenantId) {
        if (ci == null || tenantId == null) {
            return Optional.empty();
        }
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE p.ci = :ci AND p.tenantId = :tenantId",
            profesional_salud.class
        );
        query.setParameter("ci", ci);
        query.setParameter("tenantId", tenantId);
        List<profesional_salud> result = query.setMaxResults(1).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<profesional_salud> findAll() {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        return query.getResultList();
    }

    public List<profesional_salud> findByEspecialidad(String especialidad) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE p.especialidad = :especialidad ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("especialidad", especialidad);
        return query.getResultList();
    }

    public boolean existsByCiAndTenantId(Integer ci, UUID tenantId) {
        if (ci == null || tenantId == null) {
            return false;
        }
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p WHERE p.ci = :ci AND p.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("ci", ci);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    public void delete(profesional_salud profesional) {
        if (!em.contains(profesional)) {
            profesional = em.merge(profesional);
        }
        em.remove(profesional);
    }

    public void deleteByCiAndTenantId(Integer ci, UUID tenantId) {
        findByCiAndTenantId(ci, tenantId).ifPresent(this::delete);
    }

    public List<profesional_salud> findByNombreOrApellido(String searchTerm) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

    public List<profesional_salud> findAllPaginated(int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<profesional_salud> findByNombreOrApellidoPaginated(String searchTerm, int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p",
            Long.class
        );
        return query.getSingleResult();
    }

    public long countByNombreOrApellido(String searchTerm) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term)",
            Long.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getSingleResult();
    }

    /**
     * Obtiene todos los profesionales de una clínica específica (filtrado por tenant_id)
     */
    public List<profesional_salud> findByTenantIdPaginated(UUID tenantId, int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p " +
            "WHERE p.tenantId = :tenantId " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("tenantId", tenantId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Busca profesionales por nombre o apellido filtrados por clínica
     */
    public List<profesional_salud> findByNombreOrApellidoAndTenantIdPaginated(
            String searchTerm, UUID tenantId, int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p " +
            "WHERE p.tenantId = :tenantId AND " +
            "(LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term)) " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("tenantId", tenantId);
        query.setParameter("term", "%" + searchTerm + "%");
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Cuenta profesionales de una clínica específica
     */
    public long countByTenantId(UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p " +
            "WHERE p.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult();
    }

    /**
     * Cuenta profesionales que coinciden con el término de búsqueda en una clínica
     */
    public long countByNombreOrApellidoAndTenantId(String searchTerm, UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p " +
            "WHERE p.tenantId = :tenantId AND " +
            "(LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term))",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getSingleResult();
    }

    /**
     * Busca un profesional por email y clínica (tenant) para autenticación.
     */
    public Optional<profesional_salud> findByEmailAndTenant(String email, UUID tenantId) {
        if (email == null || tenantId == null) {
            return Optional.empty();
        }
        String normalizedEmail = email.trim();
        if (normalizedEmail.isEmpty()) {
            return Optional.empty();
        }
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p " +
            "WHERE LOWER(p.email) = LOWER(:email) AND p.tenantId = :tenantId",
            profesional_salud.class
        );
        query.setParameter("email", normalizedEmail);
        query.setParameter("tenantId", tenantId);
        List<profesional_salud> result = query.setMaxResults(1).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
