package com.hcen.periferico.dao;

import com.hcen.core.domain.ConfiguracionClinica;
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

    public ConfiguracionClinica save(ConfiguracionClinica config) {
        if (config.getId() == null) {
            em.persist(config);
            return config;
        } else {
            return em.merge(config);
        }
    }

    public Optional<ConfiguracionClinica> findById(UUID id) {
        ConfiguracionClinica config = em.find(ConfiguracionClinica.class, id);
        return Optional.ofNullable(config);
    }

    public Optional<ConfiguracionClinica> findByClinicaRut(String clinicaRut) {
        try {
            TypedQuery<ConfiguracionClinica> query = em.createQuery(
                "SELECT c FROM ConfiguracionClinica c WHERE c.clinicaRut = :rut",
                ConfiguracionClinica.class
            );
            query.setParameter("rut", clinicaRut);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByClinicaRut(String clinicaRut) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(c) FROM ConfiguracionClinica c WHERE c.clinicaRut = :rut",
            Long.class
        );
        query.setParameter("rut", clinicaRut);
        return query.getSingleResult() > 0;
    }

    public void delete(ConfiguracionClinica config) {
        if (!em.contains(config)) {
            config = em.merge(config);
        }
        em.remove(config);
    }

    public void deleteById(UUID id) {
        findById(id).ifPresent(this::delete);
    }

    public List<ConfiguracionClinica> findAll() {
        TypedQuery<ConfiguracionClinica> query = em.createQuery(
            "SELECT c FROM ConfiguracionClinica c ORDER BY c.clinicaRut",
            ConfiguracionClinica.class
        );
        return query.getResultList();
    }

    public ConfiguracionClinica getOrCreateDefault(String clinicaRut) {
        return findByClinicaRut(clinicaRut).orElseGet(() -> {
            ConfiguracionClinica config = new ConfiguracionClinica(clinicaRut);
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
