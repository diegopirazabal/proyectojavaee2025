package hcen.central.inus.dao;

import hcen.central.inus.entity.AdminHCEN;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Stateless
public class AdminHCENDAO {
    
    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;
    
    public AdminHCEN save(AdminHCEN admin) {
        if (admin.getId() == null) {
            em.persist(admin);
            return admin;
        } else {
            return em.merge(admin);
        }
    }
    
    public Optional<AdminHCEN> findById(Long id) {
        AdminHCEN admin = em.find(AdminHCEN.class, id);
        return Optional.ofNullable(admin);
    }
    
    public Optional<AdminHCEN> findByUsername(String username) {
        try {
            TypedQuery<AdminHCEN> query = em.createQuery(
                "SELECT a FROM AdminHCEN a WHERE a.username = :username AND a.active = true", 
                AdminHCEN.class
            );
            query.setParameter("username", username);
            AdminHCEN admin = query.getSingleResult();
            return Optional.of(admin);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    public Optional<AdminHCEN> findByEmail(String email) {
        try {
            TypedQuery<AdminHCEN> query = em.createQuery(
                "SELECT a FROM AdminHCEN a WHERE a.email = :email AND a.active = true", 
                AdminHCEN.class
            );
            query.setParameter("email", email);
            AdminHCEN admin = query.getSingleResult();
            return Optional.of(admin);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    public List<AdminHCEN> findAll() {
        TypedQuery<AdminHCEN> query = em.createQuery(
            "SELECT a FROM AdminHCEN a WHERE a.active = true ORDER BY a.createdAt DESC", 
            AdminHCEN.class
        );
        return query.getResultList();
    }
    
    public List<AdminHCEN> findAllIncludingInactive() {
        TypedQuery<AdminHCEN> query = em.createQuery(
            "SELECT a FROM AdminHCEN a ORDER BY a.createdAt DESC", 
            AdminHCEN.class
        );
        return query.getResultList();
    }
    
    public void updateLastLogin(Long adminId) {
        AdminHCEN admin = em.find(AdminHCEN.class, adminId);
        if (admin != null) {
            admin.setLastLogin(LocalDateTime.now());
            em.merge(admin);
        }
    }
    
    public void deactivate(Long adminId) {
        AdminHCEN admin = em.find(AdminHCEN.class, adminId);
        if (admin != null) {
            admin.setActive(false);
            em.merge(admin);
        }
    }
    
    public void activate(Long adminId) {
        AdminHCEN admin = em.find(AdminHCEN.class, adminId);
        if (admin != null) {
            admin.setActive(true);
            em.merge(admin);
        }
    }
    
    public boolean existsByUsername(String username) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM AdminHCEN a WHERE a.username = :username", 
            Long.class
        );
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }
    
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM AdminHCEN a WHERE a.email = :email", 
            Long.class
        );
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    public void delete(Long adminId) {
        AdminHCEN admin = em.find(AdminHCEN.class, adminId);
        if (admin != null) {
            em.remove(admin);
        }
    }
}