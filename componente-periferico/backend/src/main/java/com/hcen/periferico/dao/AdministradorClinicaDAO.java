package com.hcen.periferico.dao;

import com.hcen.core.domain.administrador_clinica;
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

    public Optional<administrador_clinica> findByUsernameAndClinica(String username, String clinicaRut) {
        try {
            TypedQuery<administrador_clinica> query = em.createQuery(
                "SELECT a FROM administrador_clinica a WHERE a.username = :username AND a.clinica = :clinica",
                administrador_clinica.class
            );
            query.setParameter("username", username);
            query.setParameter("clinica", clinicaRut);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<administrador_clinica> findByClinica(String clinicaRut) {
        TypedQuery<administrador_clinica> query = em.createQuery(
            "SELECT a FROM administrador_clinica a WHERE a.clinica = :clinica ORDER BY a.nombre, a.apellidos",
            administrador_clinica.class
        );
        query.setParameter("clinica", clinicaRut);
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

    public boolean existsByUsernameAndClinica(String username, String clinicaRut) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM administrador_clinica a WHERE a.username = :username AND a.clinica = :clinica",
            Long.class
        );
        query.setParameter("username", username);
        query.setParameter("clinica", clinicaRut);
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
            "SELECT a FROM administrador_clinica a ORDER BY a.clinica, a.nombre, a.apellidos",
            administrador_clinica.class
        );
        return query.getResultList();
    }
}
