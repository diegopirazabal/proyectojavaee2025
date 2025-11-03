package com.hcen.periferico.dao;

import com.hcen.periferico.entity.configuracion_clinica;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class ConfiguracionClinicaDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public configuracion_clinica save(configuracion_clinica config) {
        if (config.getId() == null) {
            em.persist(config);
            return config;
        } else {
            return em.merge(config);
        }
    }

    public Optional<configuracion_clinica> findById(UUID id) {
        configuracion_clinica config = em.find(configuracion_clinica.class, id);
        return Optional.ofNullable(config);
    }

    public Optional<configuracion_clinica> findByTenantId(UUID tenantId) {
        try {
            TypedQuery<configuracion_clinica> query = em.createQuery(
                "SELECT c FROM configuracion_clinica c WHERE c.tenantId = :tenantId",
                configuracion_clinica.class
            );
            query.setParameter("tenantId", tenantId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByTenantId(UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(c) FROM configuracion_clinica c WHERE c.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    public void delete(configuracion_clinica config) {
        if (!em.contains(config)) {
            config = em.merge(config);
        }
        em.remove(config);
    }

    public void deleteById(UUID id) {
        findById(id).ifPresent(this::delete);
    }

    public List<configuracion_clinica> findAll() {
        TypedQuery<configuracion_clinica> query = em.createQuery(
            "SELECT c FROM configuracion_clinica c ORDER BY c.tenantId",
            configuracion_clinica.class
        );
        return query.getResultList();
    }

    public configuracion_clinica getOrCreateDefault(UUID tenantId) {
        return findByTenantId(tenantId).orElseGet(() -> {
            configuracion_clinica config = new configuracion_clinica(tenantId);
            // Valores por defecto
            config.setColorPrimario("#007bff");
            config.setColorSecundario("#6c757d");
            config.setNombreSistema("Sistema de Gestión Clínica");
            config.setTema("default");
            config.setNodoPerifericoHabilitado(false);
            return save(config);
        });
    }
}
