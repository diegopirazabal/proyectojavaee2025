package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
     * Lista usuarios pertenecientes a una clínica
     */
    public List<UsuarioSalud> findByTenantId(UUID tenantId) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId AND u.active = true " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        return query.getResultList();
    }

    /**
     * Busca usuarios aplicando filtros opcionales
     */
    public List<UsuarioSalud> findByFilters(TipoDocumento tipoDocumento,
                                            String numeroDocumento,
                                            String nombre,
                                            String apellido,
                                            int page,
                                            int size) {
        StringBuilder jpql = new StringBuilder("SELECT u FROM UsuarioSalud u WHERE u.active = true");

        if (tipoDocumento != null && numeroDocumento != null && !numeroDocumento.isBlank()) {
            jpql.append(" AND u.tipoDeDocumento = :tipoDocumento AND u.cedula = :cedula");
        } else {
            if (nombre != null && !nombre.isBlank()) {
                jpql.append(" AND LOWER(u.primerNombre) LIKE LOWER(:nombre)");
            }
            if (apellido != null && !apellido.isBlank()) {
                jpql.append(" AND LOWER(u.primerApellido) LIKE LOWER(:apellido)");
            }
        }

        jpql.append(" ORDER BY u.primerApellido, u.primerNombre");

        TypedQuery<UsuarioSalud> query = em.createQuery(jpql.toString(), UsuarioSalud.class);

        if (tipoDocumento != null && numeroDocumento != null && !numeroDocumento.isBlank()) {
            query.setParameter("tipoDocumento", tipoDocumento);
            query.setParameter("cedula", numeroDocumento.trim());
        } else {
            if (nombre != null && !nombre.isBlank()) {
                query.setParameter("nombre", "%" + nombre.trim() + "%");
            }
            if (apellido != null && !apellido.isBlank()) {
                query.setParameter("apellido", "%" + apellido.trim() + "%");
            }
        }

        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Cuenta usuarios aplicando filtros
     */
    public long countByFilters(TipoDocumento tipoDocumento,
                               String numeroDocumento,
                               String nombre,
                               String apellido) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(u) FROM UsuarioSalud u WHERE u.active = true");

        if (tipoDocumento != null && numeroDocumento != null && !numeroDocumento.isBlank()) {
            jpql.append(" AND u.tipoDeDocumento = :tipoDocumento AND u.cedula = :cedula");
        } else {
            if (nombre != null && !nombre.isBlank()) {
                jpql.append(" AND LOWER(u.primerNombre) LIKE LOWER(:nombre)");
            }
            if (apellido != null && !apellido.isBlank()) {
                jpql.append(" AND LOWER(u.primerApellido) LIKE LOWER(:apellido)");
            }
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);

        if (tipoDocumento != null && numeroDocumento != null && !numeroDocumento.isBlank()) {
            query.setParameter("tipoDocumento", tipoDocumento);
            query.setParameter("cedula", numeroDocumento.trim());
        } else {
            if (nombre != null && !nombre.isBlank()) {
                query.setParameter("nombre", "%" + nombre.trim() + "%");
            }
            if (apellido != null && !apellido.isBlank()) {
                query.setParameter("apellido", "%" + apellido.trim() + "%");
            }
        }

        return query.getSingleResult();
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
     * Busca usuarios dentro de una clínica filtrando por nombre o apellido
     */
    public List<UsuarioSalud> searchByNombreOrApellidoAndTenantId(String searchTerm, UUID tenantId) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.tenantId = :tenantId AND u.active = true AND (" +
            "LOWER(u.primerNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoNombre) LIKE LOWER(:term) OR " +
            "LOWER(u.primerApellido) LIKE LOWER(:term) OR " +
            "LOWER(u.segundoApellido) LIKE LOWER(:term)) " +
            "ORDER BY u.primerApellido, u.primerNombre",
            UsuarioSalud.class
        );
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

    /**
     * Verifica si existe un usuario con una combinación cédula + tenant_id
     */
    public boolean existsByCedulaAndTenantId(String cedula, UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSalud u WHERE u.cedula = :cedula AND u.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        return query.getSingleResult() > 0;
    }

    /**
     * Elimina físicamente un usuario por cédula y tenant_id
     */
    public boolean deleteByCedulaAndTenantId(String cedula, UUID tenantId) {
        try {
            TypedQuery<UsuarioSalud> query = em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.cedula = :cedula AND u.tenantId = :tenantId",
                UsuarioSalud.class
            );
            query.setParameter("cedula", cedula);
            query.setParameter("tenantId", normalizeTenantId(tenantId));
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

    private String normalizeTenantId(UUID tenantId) {
        return tenantId != null ? tenantId.toString() : null;
    }
}
