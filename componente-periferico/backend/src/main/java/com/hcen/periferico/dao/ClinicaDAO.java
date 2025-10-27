package com.hcen.periferico.dao;

import com.hcen.core.domain.clinica;
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
