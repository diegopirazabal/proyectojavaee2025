package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
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
 * Operaciones CRUD sobre UsuarioSalud
 */
@Stateless
public class OIDCUserDAO {
    
    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;
    
    /**
     * Guarda o actualiza un usuario OIDC
     */
    public UsuarioSalud save(UsuarioSalud user) {
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
    public Optional<UsuarioSalud> findById(Long id) {
        UsuarioSalud user = em.find(UsuarioSalud.class, id);
        return Optional.ofNullable(user);
    }
    
    /**
     * Busca usuario por cédula - retorna Optional
     */
    public Optional<UsuarioSalud> findByCedulaOptional(String cedula) {
        try {
            TypedQuery<UsuarioSalud> query = em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.cedula = :cedula AND u.active = true",
                UsuarioSalud.class
            );
            query.setParameter("cedula", cedula);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca usuario por cédula - retorna UsuarioSalud o null
     */
    public UsuarioSalud findByCedula(String cedula) {
        return findByCedulaOptional(cedula).orElse(null);
    }
    
    /**
     * @deprecated Usar findByCedula en su lugar
     */
    @Deprecated
    public UsuarioSalud findBySub(String sub) {
        return findByCedula(sub);
    }
    
    /**
     * Busca usuario por email
     */
    public Optional<UsuarioSalud> findByEmail(String email) {
        try {
            TypedQuery<UsuarioSalud> query = em.createQuery(
                "SELECT u FROM UsuarioSalud u WHERE u.email = :email AND u.active = true",
                UsuarioSalud.class
            );
            query.setParameter("email", email);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca usuario por cédula (alias de findByCedulaOptional)
     */
    public Optional<UsuarioSalud> findByDocumento(String cedula) {
        return findByCedulaOptional(cedula);
    }
    
    /**
     * Lista todos los usuarios activos
     */
    public List<UsuarioSalud> findAll() {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.active = true ORDER BY u.createdAt DESC",
            UsuarioSalud.class
        );
        return query.getResultList();
    }
    
    /**
     * Lista todos los usuarios (incluyendo inactivos)
     */
    public List<UsuarioSalud> findAllIncludingInactive() {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u ORDER BY u.createdAt DESC",
            UsuarioSalud.class
        );
        return query.getResultList();
    }
    
    /**
     * Actualiza el último login del usuario
     */
    public void updateLastLogin(String cedula) {
        UsuarioSalud user = findByCedula(cedula);
        if (user != null) {
            user.setLastLogin(Instant.now());
            em.merge(user);
        }
    }
    
    /**
     * Desactiva un usuario
     */
    public void deactivate(String cedula) {
        UsuarioSalud user = findByCedula(cedula);
        if (user != null) {
            user.setActive(false);
            em.merge(user);
        }
    }
    
    /**
     * Activa un usuario
     */
    public void activate(String cedula) {
        TypedQuery<UsuarioSalud> query = em.createQuery(
            "SELECT u FROM UsuarioSalud u WHERE u.cedula = :cedula",
            UsuarioSalud.class
        );
        query.setParameter("cedula", cedula);
        try {
            UsuarioSalud user = query.getSingleResult();
            user.setActive(true);
            em.merge(user);
        } catch (NoResultException e) {
            // Usuario no existe
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
     * @deprecated Usar existsByCedula en su lugar
     */
    @Deprecated
    public boolean existsBySub(String sub) {
        return existsByCedula(sub);
    }
    
    /**
     * Verifica si existe un usuario con el email dado
     */
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM UsuarioSalud u WHERE u.email = :email",
            Long.class
        );
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Elimina un usuario (hard delete)
     */
    public void delete(String cedula) {
        UsuarioSalud user = findByCedula(cedula);
        if (user != null) {
            em.remove(user);
        }
    }
}
