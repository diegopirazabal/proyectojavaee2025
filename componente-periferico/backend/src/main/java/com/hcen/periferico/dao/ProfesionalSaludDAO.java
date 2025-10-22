package com.hcen.periferico.dao;

<<<<<<< HEAD
import com.hcen.core.domain.ProfesionalSalud;
=======
import com.hcen.core.domain.profesional_salud;
>>>>>>> main
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

<<<<<<< HEAD
    public ProfesionalSalud save(ProfesionalSalud profesional) {
=======
    public profesional_salud save(profesional_salud profesional) {
>>>>>>> main
        if (profesional.getCi() == null || !em.contains(profesional)) {
            return em.merge(profesional);
        } else {
            return em.merge(profesional);
        }
    }

<<<<<<< HEAD
    public Optional<ProfesionalSalud> findByCi(Integer ci) {
        ProfesionalSalud profesional = em.find(ProfesionalSalud.class, ci);
        return Optional.ofNullable(profesional);
    }

    public List<ProfesionalSalud> findAll() {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
=======
    public Optional<profesional_salud> findByCi(Integer ci) {
        profesional_salud profesional = em.find(profesional_salud.class, ci);
        return Optional.ofNullable(profesional);
    }

    public List<profesional_salud> findAll() {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
>>>>>>> main
        );
        return query.getResultList();
    }

<<<<<<< HEAD
    public List<ProfesionalSalud> findByEspecialidad(String especialidad) {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p WHERE p.especialidad = :especialidad ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
=======
    public List<profesional_salud> findByEspecialidad(String especialidad) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE p.especialidad = :especialidad ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
>>>>>>> main
        );
        query.setParameter("especialidad", especialidad);
        return query.getResultList();
    }

    public boolean existsByCi(Integer ci) {
        TypedQuery<Long> query = em.createQuery(
<<<<<<< HEAD
            "SELECT COUNT(p) FROM ProfesionalSalud p WHERE p.ci = :ci",
=======
            "SELECT COUNT(p) FROM profesional_salud p WHERE p.ci = :ci",
>>>>>>> main
            Long.class
        );
        query.setParameter("ci", ci);
        return query.getSingleResult() > 0;
    }

<<<<<<< HEAD
    public void delete(ProfesionalSalud profesional) {
=======
    public void delete(profesional_salud profesional) {
>>>>>>> main
        if (!em.contains(profesional)) {
            profesional = em.merge(profesional);
        }
        em.remove(profesional);
    }

    public void deleteByCi(Integer ci) {
        findByCi(ci).ifPresent(this::delete);
    }

<<<<<<< HEAD
    public List<ProfesionalSalud> findByNombreOrApellido(String searchTerm) {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
=======
    public List<profesional_salud> findByNombreOrApellido(String searchTerm) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
>>>>>>> main
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

<<<<<<< HEAD
    public List<ProfesionalSalud> findAllPaginated(int page, int size) {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
=======
    public List<profesional_salud> findAllPaginated(int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
>>>>>>> main
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

<<<<<<< HEAD
    public List<ProfesionalSalud> findByNombreOrApellidoPaginated(String searchTerm, int page, int size) {
        TypedQuery<ProfesionalSalud> query = em.createQuery(
            "SELECT p FROM ProfesionalSalud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            ProfesionalSalud.class
=======
    public List<profesional_salud> findByNombreOrApellidoPaginated(String searchTerm, int page, int size) {
        TypedQuery<profesional_salud> query = em.createQuery(
            "SELECT p FROM profesional_salud p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term) " +
            "ORDER BY p.apellidos, p.nombre",
            profesional_salud.class
>>>>>>> main
        );
        query.setParameter("term", "%" + searchTerm + "%");
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
<<<<<<< HEAD
            "SELECT COUNT(p) FROM ProfesionalSalud p",
=======
            "SELECT COUNT(p) FROM profesional_salud p",
>>>>>>> main
            Long.class
        );
        return query.getSingleResult();
    }

    public long countByNombreOrApellido(String searchTerm) {
        TypedQuery<Long> query = em.createQuery(
<<<<<<< HEAD
            "SELECT COUNT(p) FROM ProfesionalSalud p WHERE " +
=======
            "SELECT COUNT(p) FROM profesional_salud p WHERE " +
>>>>>>> main
            "LOWER(p.nombre) LIKE LOWER(:term) OR LOWER(p.apellidos) LIKE LOWER(:term)",
            Long.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getSingleResult();
    }
}
