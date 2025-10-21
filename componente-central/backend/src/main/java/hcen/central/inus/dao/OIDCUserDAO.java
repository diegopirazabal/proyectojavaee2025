package hcen.central.inus.dao;

import hcen.central.inus.entity.OIDCUser;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * DAO para persistencia de usuarios autenticados via OIDC
 * Operaciones CRUD sobre OIDCUser
 */
@Stateless
public class OIDCUserDAO {
    
    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;
    
    /**
     * Guarda o actualiza un usuario OIDC
     */
    public OIDCUser save(OIDCUser user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        } else {
            return em.merge(user);
        }
    }
    
    /**
     * Busca usuario por ID
     */
    public Optional<OIDCUser> findById(Long id) {
        OIDCUser user = em.find(OIDCUser.class, id);
        return Optional.ofNullable(user);
    }
    
    /**
     * Busca usuario por subject (identificador de gub.uy) - retorna Optional
     */
    public Optional<OIDCUser> findBySubOptional(String sub) {
        try {
            TypedQuery<OIDCUser> query = em.createQuery(
                "SELECT u FROM OIDCUser u WHERE u.sub = :sub AND u.active = true",
                OIDCUser.class
            );
            query.setParameter("sub", sub);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca usuario por subject (identificador de gub.uy) - retorna OIDCUser o null
     */
    public OIDCUser findBySub(String sub) {
        return findBySubOptional(sub).orElse(null);
    }
    
    /**
     * Busca usuario por email
     */
    public Optional<OIDCUser> findByEmail(String email) {
        try {
            TypedQuery<OIDCUser> query = em.createQuery(
                "SELECT u FROM OIDCUser u WHERE u.email = :email AND u.active = true",
                OIDCUser.class
            );
            query.setParameter("email", email);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca usuario por número de documento
     */
    public Optional<OIDCUser> findByDocumento(String numeroDocumento) {
        try {
            TypedQuery<OIDCUser> query = em.createQuery(
                "SELECT u FROM OIDCUser u WHERE u.numeroDocumento = :doc AND u.active = true",
                OIDCUser.class
            );
            query.setParameter("doc", numeroDocumento);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista todos los usuarios activos
     */
    public List<OIDCUser> findAll() {
        TypedQuery<OIDCUser> query = em.createQuery(
            "SELECT u FROM OIDCUser u WHERE u.active = true ORDER BY u.createdAt DESC",
            OIDCUser.class
        );
        return query.getResultList();
    }
    
    /**
     * Lista todos los usuarios (incluyendo inactivos)
     */
    public List<OIDCUser> findAllIncludingInactive() {
        TypedQuery<OIDCUser> query = em.createQuery(
            "SELECT u FROM OIDCUser u ORDER BY u.createdAt DESC",
            OIDCUser.class
        );
        return query.getResultList();
    }
    
    /**
     * Actualiza el último login del usuario
     */
    public void updateLastLogin(String sub) {
        OIDCUser user = findBySub(sub);
        if (user != null) {
            user.setLastLogin(Instant.now());
            em.merge(user);
        }
    }
    
    /**
     * Desactiva un usuario
     */
    public void deactivate(String sub) {
        OIDCUser user = findBySub(sub);
        if (user != null) {
            user.setActive(false);
            em.merge(user);
        }
    }
    
    /**
     * Activa un usuario
     */
    public void activate(String sub) {
        TypedQuery<OIDCUser> query = em.createQuery(
            "SELECT u FROM OIDCUser u WHERE u.sub = :sub",
            OIDCUser.class
        );
        query.setParameter("sub", sub);
        try {
            OIDCUser user = query.getSingleResult();
            user.setActive(true);
            em.merge(user);
        } catch (NoResultException e) {
            // Usuario no existe
        }
    }
    
    /**
     * Verifica si existe un usuario con el sub dado
     */
    public boolean existsBySub(String sub) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM OIDCUser u WHERE u.sub = :sub",
            Long.class
        );
        query.setParameter("sub", sub);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Verifica si existe un usuario con el email dado
     */
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM OIDCUser u WHERE u.email = :email",
            Long.class
        );
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Elimina un usuario (hard delete)
     */
    public void delete(String sub) {
        OIDCUser user = findBySub(sub);
        if (user != null) {
            em.remove(user);
        }
    }
}
