package com.hcen.periferico.dao;

import com.hcen.periferico.entity.clinica;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class ClinicaDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public clinica save(clinica clinica) {
        if (clinica.getTenantId() == null) {
            em.persist(clinica);
            return clinica;
        } else {
            return em.merge(clinica);
        }
    }

    public Optional<clinica> findByTenantId(UUID tenantId) {
        clinica c = em.find(clinica.class, tenantId);
        return Optional.ofNullable(c);
    }

    public Optional<clinica> findByNombreIgnoreCase(String nombre) {
        TypedQuery<clinica> query = em.createQuery(
            "SELECT c FROM clinica c WHERE LOWER(c.nombre) = :nombre",
            clinica.class
        );
        query.setParameter("nombre", nombre != null ? nombre.toLowerCase(java.util.Locale.ROOT) : null);
        List<clinica> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<clinica> findAll() {
        TypedQuery<clinica> query = em.createQuery(
            "SELECT c FROM clinica c ORDER BY c.nombre",
            clinica.class
        );
        return query.getResultList();
    }

    public void delete(clinica clinica) {
        if (!em.contains(clinica)) {
            clinica = em.merge(clinica);
        }
        em.remove(clinica);
    }

    public void deleteByTenantId(UUID tenantId) {
        findByTenantId(tenantId).ifPresent(this::delete);
    }

    /**
     * Obtiene nombres de múltiples clínicas en una sola query (batch)
     * Optimizado para evitar N consultas individuales
     *
     * @param tenantIds Colección de tenant IDs
     * @return Map con tenantId -> nombreClinica (solo incluye los que existen en BD)
     */
    public java.util.Map<UUID, String> getNombresClinicasBatch(java.util.Collection<UUID> tenantIds) {
        java.util.Map<UUID, String> resultado = new java.util.HashMap<>();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return resultado;
        }

        try {
            // Filtrar nulls y crear lista de IDs únicos
            java.util.List<UUID> ids = tenantIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

            if (ids.isEmpty()) {
                return resultado;
            }

            // UNA sola consulta con IN clause
            TypedQuery<clinica> query = em.createQuery(
                "SELECT c FROM clinica c WHERE c.tenantId IN :ids",
                clinica.class
            );
            query.setParameter("ids", ids);
            List<clinica> clinicas = query.getResultList();

            // Mapear tenantId -> nombre
            for (clinica c : clinicas) {
                resultado.put(c.getTenantId(), c.getNombre());
            }
        } catch (Exception e) {
            // Log error si es necesario
            e.printStackTrace();
        }
        return resultado;
    }

    /**
     * Cuenta el total de clínicas registradas
     * Usado para paginación lazy (rowCount)
     *
     * @return Cantidad total de clínicas
     */
    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(c) FROM clinica c",
            Long.class
        );
        return query.getSingleResult();
    }

    /**
     * Obtiene clínicas con paginación
     * Usado para lazy loading de tabla de reportes
     *
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Lista de clínicas de la página solicitada, ordenadas por nombre
     */
    public List<clinica> findAllPaginated(int page, int size) {
        TypedQuery<clinica> query = em.createQuery(
            "SELECT c FROM clinica c ORDER BY c.nombre",
            clinica.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }
}
