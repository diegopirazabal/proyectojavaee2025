package com.hcen.periferico.dao;

import com.hcen.core.domain.AdministradorClinica;
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

    public AdministradorClinica save(AdministradorClinica admin) {
        if (admin.getId() == null) {
            em.persist(admin);
            return admin;
        } else {
            return em.merge(admin);
        }
    }

    public Optional<AdministradorClinica> findById(UUID id) {
        AdministradorClinica admin = em.find(AdministradorClinica.class, id);
        return Optional.ofNullable(admin);
    }

    public Optional<AdministradorClinica> findByUsernameAndClinica(String username, String clinicaRut) {
        try {
            TypedQuery<AdministradorClinica> query = em.createQuery(
                "SELECT a FROM AdministradorClinica a WHERE a.username = :username AND a.clinica = :clinica",
                AdministradorClinica.class
            );
            query.setParameter("username", username);
            query.setParameter("clinica", clinicaRut);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<AdministradorClinica> findByClinica(String clinicaRut) {
        TypedQuery<AdministradorClinica> query = em.createQuery(
            "SELECT a FROM AdministradorClinica a WHERE a.clinica = :clinica ORDER BY a.nombre, a.apellidos",
            AdministradorClinica.class
        );
        query.setParameter("clinica", clinicaRut);
        return query.getResultList();
    }

    public List<AdministradorClinica> findByUsername(String username) {
        TypedQuery<AdministradorClinica> query = em.createQuery(
            "SELECT a FROM AdministradorClinica a WHERE a.username = :username",
            AdministradorClinica.class
        );
        query.setParameter("username", username);
        return query.getResultList();
    }

    public boolean existsByUsernameAndClinica(String username, String clinicaRut) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM AdministradorClinica a WHERE a.username = :username AND a.clinica = :clinica",
            Long.class
        );
        query.setParameter("username", username);
        query.setParameter("clinica", clinicaRut);
        return query.getSingleResult() > 0;
    }

    public void delete(AdministradorClinica admin) {
        if (!em.contains(admin)) {
            admin = em.merge(admin);
        }
        em.remove(admin);
    }

    public void deleteById(UUID id) {
        findById(id).ifPresent(this::delete);
    }

    public List<AdministradorClinica> findAll() {
        TypedQuery<AdministradorClinica> query = em.createQuery(
            "SELECT a FROM AdministradorClinica a ORDER BY a.clinica, a.nombre, a.apellidos",
            AdministradorClinica.class
        );
        return query.getResultList();
    }
}
