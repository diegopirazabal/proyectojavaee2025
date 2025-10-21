package hcen.central.inus.dao;

import hcen.central.inus.entity.admin_hcen;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Stateless
public class admin_hcen_dao {
    
    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;
    
    public admin_hcen save(admin_hcen admin) {
        if (admin.getId() == null) {
            em.persist(admin);
            return admin;
        } else {
            return em.merge(admin);
        }
    }
    
    public Optional<admin_hcen> findById(Long id) {
        admin_hcen admin = em.find(admin_hcen.class, id);
        return Optional.ofNullable(admin);
    }
    
    public Optional<admin_hcen> findByUsername(String username) {
        try {
            TypedQuery<admin_hcen> query = em.createQuery(
                "SELECT a FROM admin_hcen a WHERE a.username = :username AND a.active = true", 
                admin_hcen.class
            );
            query.setParameter("username", username);
            admin_hcen admin = query.getSingleResult();
            return Optional.of(admin);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    public Optional<admin_hcen> findByEmail(String email) {
        try {
            TypedQuery<admin_hcen> query = em.createQuery(
                "SELECT a FROM admin_hcen a WHERE a.email = :email AND a.active = true", 
                admin_hcen.class
            );
            query.setParameter("email", email);
            admin_hcen admin = query.getSingleResult();
            return Optional.of(admin);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<admin_hcen> findByUsernameIncludingInactive(String username) {
        try {
            TypedQuery<admin_hcen> query = em.createQuery(
                "SELECT a FROM admin_hcen a WHERE a.username = :username", 
                admin_hcen.class
            );
            query.setParameter("username", username);
            admin_hcen admin = query.getSingleResult();
            return Optional.of(admin);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    public List<admin_hcen> findAll() {
        TypedQuery<admin_hcen> query = em.createQuery(
            "SELECT a FROM admin_hcen a WHERE a.active = true ORDER BY a.createdAt DESC", 
            admin_hcen.class
        );
        return query.getResultList();
    }
    
    public List<admin_hcen> findAllIncludingInactive() {
        TypedQuery<admin_hcen> query = em.createQuery(
            "SELECT a FROM admin_hcen a ORDER BY a.createdAt DESC", 
            admin_hcen.class
        );
        return query.getResultList();
    }
    
    public void updateLastLogin(Long adminId) {
        admin_hcen admin = em.find(admin_hcen.class, adminId);
        if (admin != null) {
            admin.setLastLogin(LocalDateTime.now());
            em.merge(admin);
        }
    }
    
    public void deactivate(Long adminId) {
        admin_hcen admin = em.find(admin_hcen.class, adminId);
        if (admin != null) {
            admin.setActive(false);
            em.merge(admin);
        }
    }
    
    public void activate(Long adminId) {
        admin_hcen admin = em.find(admin_hcen.class, adminId);
        if (admin != null) {
            admin.setActive(true);
            em.merge(admin);
        }
    }
    
    public boolean existsByUsername(String username) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM admin_hcen a WHERE a.username = :username", 
            Long.class
        );
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }
    
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM admin_hcen a WHERE a.email = :email", 
            Long.class
        );
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    public void delete(Long adminId) {
        admin_hcen admin = em.find(admin_hcen.class, adminId);
        if (admin != null) {
            em.remove(admin);
        }
    }
}
