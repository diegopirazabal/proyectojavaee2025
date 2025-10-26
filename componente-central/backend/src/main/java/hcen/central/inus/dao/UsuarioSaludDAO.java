package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD sobre UsuarioSalud
 * Usado para gestión de usuarios desde clínicas periféricas
 */
@Stateless
public class UsuarioSaludDAO {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    /**
     * Guarda o actualiza un usuario
     */
    public UsuarioSalud save(UsuarioSalud usuario) {
        if (usuario.getId() == null) {
            em.persist(usuario);
            return usuario;
        } else {
            return em.merge(usuario);
        }
    }

    /**
     * Busca usuario por cédula
     */
    public Optional<UsuarioSalud> findByCedula(String cedula) {
        try {
            TypedQuery<UsuarioSalud> query = em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.cedula = :cedula",
                UsuarioSalud.class
            );
            query.setParameter("cedula", cedula);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Verifica si existe un usuario con la cédula dada
     */
    public boolean existsByCedula(String cedula) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSalud u WHERE u.cedula = :cedula",
            Long.class
        );
        query.setParameter("cedula", cedula);
        return query.getSingleResult() > 0;
    }

    /**
     * Busca usuario por ID
     */
    public Optional<UsuarioSalud> findById(Long id) {
        UsuarioSalud usuario = em.find(UsuarioSalud.class, id);
        return Optional.ofNullable(usuario);
    }

    /**
     * Lista todos los usuarios activos
     */
    public List<UsuarioSalud> findAllActive() {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.active = true ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        return query.getResultList();
    }

    /**
     * Lista usuarios paginados
     */
    public List<UsuarioSalud> findAllPaginated(int page, int size) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.active = true ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Cuenta total de usuarios activos
     */
    public long countAllActive() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSalud u WHERE u.active = true",
            Long.class
        );
        return query.getSingleResult();
    }

    /**
     * Lista todos los usuarios de una clínica (por tenant_id)
     */
    public List<UsuarioSalud> findByTenantId(java.util.UUID tenantId) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId AND u.active = true " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Busca usuarios por nombre o apellido
     */
    public List<UsuarioSalud> searchByNombreOrApellido(String searchTerm) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.active = true AND (" +
            "LOWER(u.primerNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.primerApellido) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoApellido) LIKE LOWER(:term)) " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

    /**
     * Busca usuarios por nombre o apellido filtrados por tenant_id
     */
    public List<UsuarioSalud> searchByNombreOrApellidoAndTenantId(String searchTerm, java.util.UUID tenantId) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId AND u.active = true AND (" +
            "LOWER(u.primerNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.primerApellido) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoApellido) LIKE LOWER(:term)) " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Verifica si existe un usuario con la combinación cédula + tenant_id
     */
    public boolean existsByCedulaAndTenantId(String cedula, java.util.UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSalud u WHERE u.cedula = :cedula AND u.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    /**
     * Elimina físicamente un usuario por la combinación cédula + tenant_id
     */
    public boolean deleteByCedulaAndTenantId(String cedula, java.util.UUID tenantId) {
        try {
            TypedQuery<UsuarioSalud> query = em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.cedula = :cedula AND u.tenantId = :tenantId",
                UsuarioSalud.class
            );
            query.setParameter("cedula", cedula);
            query.setParameter("tenantId", tenantId);
            UsuarioSalud usuario = query.getSingleResult();
            em.remove(usuario);
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    /**
     * Desactiva un usuario (soft delete)
     */
    public void deactivate(String cedula) {
        Optional<UsuarioSalud> usuario = findByCedula(cedula);
        usuario.ifPresent(u -> {
            u.setActive(false);
            em.merge(u);
        });
    }
}
