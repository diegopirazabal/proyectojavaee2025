package com.hcen.periferico.dao;

import com.hcen.periferico.entity.administrador_clinica;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class AdministradorClinicaDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public administrador_clinica save(administrador_clinica admin) {
        if (admin.getId() == null) {
            em.persist(admin);
            return admin;
        } else {
            return em.merge(admin);
        }
    }

    public Optional<administrador_clinica> findById(UUID id) {
        administrador_clinica admin = em.find(administrador_clinica.class, id);
        return Optional.ofNullable(admin);
    }

    public Optional<administrador_clinica> findByUsernameAndTenant(String username, UUID tenantId) {
        try {
            TypedQuery<administrador_clinica> query = em.createQuery(
                "SELECT a FROM administrador_clinica a WHERE a.username = :username AND a.tenantId = :tenantId",
                administrador_clinica.class
            );
            query.setParameter("username", username);
            query.setParameter("tenantId", tenantId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<administrador_clinica> findByTenant(UUID tenantId) {
        TypedQuery<administrador_clinica> query = em.createQuery(
            "SELECT a FROM administrador_clinica a WHERE a.tenantId = :tenantId ORDER BY a.nombre, a.apellidos",
            administrador_clinica.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    public List<administrador_clinica> findByTenantPaginated(UUID tenantId, int page, int size) {
        TypedQuery<administrador_clinica> query = em.createQuery(
            "SELECT a FROM administrador_clinica a WHERE a.tenantId = :tenantId ORDER BY a.nombre, a.apellidos",
            administrador_clinica.class
        );
        query.setParameter("tenantId", tenantId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<administrador_clinica> findByUsername(String username) {
        TypedQuery<administrador_clinica> query = em.createQuery(
            "SELECT a FROM administrador_clinica a WHERE a.username = :username",
            administrador_clinica.class
        );
        query.setParameter("username", username);
        return query.getResultList();
    }

    public List<administrador_clinica> findAllPaginated(int page, int size) {
        TypedQuery<administrador_clinica> query = em.createQuery(
            "SELECT a FROM administrador_clinica a ORDER BY a.tenantId, a.nombre, a.apellidos",
            administrador_clinica.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<administrador_clinica> search(String term, UUID tenantId, int page, int size) {
        StringBuilder jpql = new StringBuilder(
            "SELECT a FROM administrador_clinica a WHERE "
            + "(LOWER(a.nombre) LIKE LOWER(:term) OR LOWER(a.apellidos) LIKE LOWER(:term) "
            + "OR LOWER(a.username) LIKE LOWER(:term))"
        );
        if (tenantId != null) {
            jpql.append(" AND a.tenantId = :tenantId");
        }
        jpql.append(" ORDER BY a.nombre, a.apellidos");

        TypedQuery<administrador_clinica> query = em.createQuery(jpql.toString(), administrador_clinica.class);
        query.setParameter("term", "%" + term + "%");
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public boolean existsByUsernameAndTenant(String username, UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM administrador_clinica a WHERE a.username = :username AND a.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("username", username);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    public boolean existsByUsernameAndTenantExcluding(String username, UUID tenantId, UUID excludeId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM administrador_clinica a WHERE a.username = :username AND a.tenantId = :tenantId AND a.id <> :excludeId",
            Long.class
        );
        query.setParameter("username", username);
        query.setParameter("tenantId", tenantId);
        query.setParameter("excludeId", excludeId);
        return query.getSingleResult() > 0;
    }

    public void delete(administrador_clinica admin) {
        if (!em.contains(admin)) {
            admin = em.merge(admin);
        }
        em.remove(admin);
    }

    public void deleteById(UUID id) {
        findById(id).ifPresent(this::delete);
    }

    public List<administrador_clinica> findAll() {
        TypedQuery<administrador_clinica> query = em.createQuery(
            "SELECT a FROM administrador_clinica a ORDER BY a.tenantId, a.nombre, a.apellidos",
            administrador_clinica.class
        );
        return query.getResultList();
    }

    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM administrador_clinica a",
            Long.class
        );
        return query.getSingleResult();
    }

    public long countByTenant(UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM administrador_clinica a WHERE a.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult();
    }

    public long countBySearch(String term, UUID tenantId) {
        StringBuilder jpql = new StringBuilder(
            "SELECT COUNT(a) FROM administrador_clinica a WHERE "
            + "(LOWER(a.nombre) LIKE LOWER(:term) OR LOWER(a.apellidos) LIKE LOWER(:term) "
            + "OR LOWER(a.username) LIKE LOWER(:term))"
        );
        if (tenantId != null) {
            jpql.append(" AND a.tenantId = :tenantId");
        }
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        query.setParameter("term", "%" + term + "%");
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.getSingleResult();
    }
}
