package com.hcen.periferico.dao;

import com.hcen.periferico.entity.profesional_salud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.*;
import java.util.stream.Collectors;

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
            "SELECT p FROM profesional_salud p WHERE p.ci = :ci AND p.tenantId = :tenantId AND p.active = true",
            profesional_salud.class
        );
        query.setParameter("ci", ci);
        query.setParameter("tenantId", tenantId);
        List<profesional_salud> result = query.setMaxResults(1).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<profesional_salud> findAll() {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE p.active = true ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        return query.getResultList();
    }

    public List<profesional_salud> findByEspecialidadId(UUID especialidadId) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE p.especialidadId = :especialidadId AND p.active = true ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("especialidadId", especialidadId);
        return query.getResultList();
    }

    public boolean existsByCiAndTenantId(Integer ci, UUID tenantId) {
        if (ci == null || tenantId == null) {
            return false;
        }
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p WHERE p.ci = :ci AND p.tenantId = :tenantId AND p.active = true",
            Long.class
        );
        query.setParameter("ci", ci);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    /**
     * Borrado lógico de profesional (soft delete)
     */
    public void softDelete(Integer ci, UUID tenantId) {
        findByCiAndTenantId(ci, tenantId).ifPresent(profesional -> {
            profesional.setActive(false);
            em.merge(profesional);
        });
    }

    public List<profesional_salud> findByNombreOrApellido(String searchTerm) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE " +
            "p.active = true AND (LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term)) " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

    public List<profesional_salud> findAllPaginated(int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE p.active = true ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<profesional_salud> findByNombreOrApellidoPaginated(String searchTerm, int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE " +
            "p.active = true AND (LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term)) " +
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
            "SELECT COUNT(p) FROM profesional_salud p WHERE p.active = true",
            Long.class
        );
        return query.getSingleResult();
    }

    public long countByNombreOrApellido(String searchTerm) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM profesional_salud p WHERE " +
            "p.active = true AND (LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term))",
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
            "WHERE p.tenantId = :tenantId AND p.active = true " +
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
            "WHERE p.tenantId = :tenantId AND p.active = true AND " +
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
            "WHERE p.tenantId = :tenantId AND p.active = true",
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
            "WHERE p.tenantId = :tenantId AND p.active = true AND " +
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
            "WHERE LOWER(p.email) = LOWER(:email) AND p.tenantId = :tenantId AND p.active = true",
            profesional_salud.class
        );
        query.setParameter("email", normalizedEmail);
        query.setParameter("tenantId", tenantId);
        List<profesional_salud> result = query.setMaxResults(1).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Cuenta profesionales activos agrupados por tenant_id (consulta agregada)
     * Optimizado para evitar N+1 queries en reportes
     *
     * @param tenantIds Colección de tenant IDs a consultar
     * @return Map con tenantId -> cantidad de profesionales activos
     */
    public Map<UUID, Long> countByTenantIdBatch(Collection<UUID> tenantIds) {
        Map<UUID, Long> resultado = new HashMap<>();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return resultado;
        }

        try {
            List<UUID> ids = tenantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            if (ids.isEmpty()) {
                return resultado;
            }

            TypedQuery<Object[]> query = em.createQuery(
                "SELECT p.tenantId, COUNT(p) FROM profesional_salud p " +
                "WHERE p.tenantId IN :tenantIds AND p.active = true " +
                "GROUP BY p.tenantId",
                Object[].class
            );
            query.setParameter("tenantIds", ids);
            List<Object[]> results = query.getResultList();

            for (Object[] row : results) {
                UUID tenantId = (UUID) row[0];
                Long count = (Long) row[1];
                resultado.put(tenantId, count);
            }
        } catch (Exception e) {
            // Log error
            e.printStackTrace();
        }
        return resultado;
    }
}
