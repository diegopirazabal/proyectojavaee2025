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
}
