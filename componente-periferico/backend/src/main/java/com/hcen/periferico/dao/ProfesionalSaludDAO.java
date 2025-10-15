package com.hcen.periferico.dao;

import com.hcen.core.domain.ProfesionalSalud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@Stateless
public class ProfesionalSaludDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public ProfesionalSalud save(ProfesionalSalud profesional) {
        if (profesional.getCi() == null || !em.contains(profesional)) {
            return em.merge(profesional);
        } else {
            return em.merge(profesional);
        }
    }

    public Optional<ProfesionalSalud> findByCi(Integer ci) {
        ProfesionalSalud profesional = em.find(ProfesionalSalud.class, ci);
        return Optional.ofNullable(profesional);
    }

    public List<ProfesionalSalud> findAll() {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
        );
        return query.getResultList();
    }

    public List<ProfesionalSalud> findByEspecialidad(String especialidad) {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p WHERE p.especialidad = :especialidad ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
        );
        query.setParameter("especialidad", especialidad);
        return query.getResultList();
    }

    public boolean existsByCi(Integer ci) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM ProfesionalSalud p WHERE p.ci = :ci",
            Long.class
        );
        query.setParameter("ci", ci);
        return query.getSingleResult() > 0;
    }

    public void delete(ProfesionalSalud profesional) {
        if (!em.contains(profesional)) {
            profesional = em.merge(profesional);
        }
        em.remove(profesional);
    }

    public void deleteByCi(Integer ci) {
        findByCi(ci).ifPresent(this::delete);
    }

    public List<ProfesionalSalud> findByNombreOrApellido(String searchTerm) {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }
}
