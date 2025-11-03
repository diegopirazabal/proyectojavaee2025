package com.hcen.periferico.dao;

import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.entity.UsuarioSalud.UsuarioSaludId;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * DAO para gestionar usuarios de salud en el componente periférico.
 * Usa la entidad UsuarioSalud con soporte multi-tenant (tenant_id).
 *
 * IMPORTANTE: Esta es la fuente de verdad local. No delega al central.
 */
@Stateless
public class UsuarioSaludDAO {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludDAO.class.getName());

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    /**
     * Persiste o actualiza un usuario
     */
    public UsuarioSalud save(UsuarioSalud usuario) {
        if (usuario.getCedula() == null || usuario.getTenantId() == null) {
            throw new IllegalArgumentException("Cédula y tenant_id son requeridos");
        }
        return em.merge(usuario);
    }

    /**
     * Busca un usuario por cédula y tenant_id (composite key)
     */
    public Optional<UsuarioSalud> findByCedulaAndTenant(String cedula, UUID tenantId) {
        UsuarioSaludId id = new UsuarioSaludId(cedula, tenantId);
        UsuarioSalud usuario = em.find(UsuarioSalud.class, id);
        return Optional.ofNullable(usuario);
    }

    /**
     * Verifica si existe un usuario con esa cédula en esa clínica
     */
    public boolean existsByCedulaAndTenant(String cedula, UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSalud u WHERE u.cedula = :cedula AND u.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    /**
     * Obtiene todos los usuarios activos de una clínica (tenant_id)
     */
    public List<UsuarioSalud> findByTenantId(UUID tenantId) {
        return findByTenantId(tenantId, true);
    }

    /**
     * Obtiene usuarios de una clínica con filtro de activos/inactivos
     */
    public List<UsuarioSalud> findByTenantId(UUID tenantId, boolean soloActivos) {
        String jpql = "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId";
        if (soloActivos) {
            jpql += " AND u.active = true";
        }
        jpql += " ORDER BY u.primerApellido, u.primerNombre";

        TypedQuery<UsuarioSalud> query = em.createQuery(jpql, UsuarioSalud.class);
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Busca usuarios por nombre o apellido dentro de una clínica
     */
    public List<UsuarioSalud> searchByNombreOrApellidoAndTenant(String searchTerm, UUID tenantId) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId AND u.active = true AND " +
            "(LOWER(u.primerNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.primerApellido) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoApellido) LIKE LOWER(:term)) " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("tenantId", tenantId);
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

    /**
     * Busca usuarios pendientes de sincronización con el central
     */
    public List<UsuarioSalud> findPendientesSincronizacion() {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.sincronizadoCentral = false AND u.active = true " +
            "ORDER BY u.createdAt",
            UsuarioSalud.class
        );
        return query.getResultList();
    }

    /**
     * Busca usuarios pendientes de sincronización para un tenant específico
     */
    public List<UsuarioSalud> findPendientesSincronizacionByTenant(UUID tenantId) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u " +
            "WHERE u.tenantId = :tenantId AND u.sincronizadoCentral = false AND u.active = true " +
            "ORDER BY u.createdAt",
            UsuarioSalud.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Marca un usuario como sincronizado con el central
     */
    public void marcarComoSincronizado(String cedula, UUID tenantId) {
        findByCedulaAndTenant(cedula, tenantId).ifPresent(usuario -> {
            usuario.setSincronizadoCentral(true);
            em.merge(usuario);
        });
    }

    /**
     * Desactivar usuario (soft delete)
     */
    public void desactivar(String cedula, UUID tenantId) {
        findByCedulaAndTenant(cedula, tenantId).ifPresent(usuario -> {
            usuario.setActive(false);
            em.merge(usuario);
        });
    }

    /**
     * Eliminar usuario físicamente (hard delete)
     */
    public void delete(String cedula, UUID tenantId) {
        findByCedulaAndTenant(cedula, tenantId).ifPresent(usuario -> {
            em.remove(usuario);
        });
    }

    /**
     * Cuenta usuarios activos por tenant
     */
    public long countByTenantId(UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSaludPeriferico u WHERE u.tenantId = :tenantId AND u.active = true",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult();
    }

    /**
     * Paginación: usuarios por tenant
     */
    public List<UsuarioSalud> findByTenantIdPaginated(UUID tenantId, int page, int size) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId AND u.active = true " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("tenantId", tenantId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Busca usuarios por cédula (sin filtro de tenant) - útil para verificación global
     */
    public List<UsuarioSalud> findByCedula(String cedula) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.cedula = :cedula",
            UsuarioSalud.class
        );
        query.setParameter("cedula", cedula);
        return query.getResultList();
    }
}
