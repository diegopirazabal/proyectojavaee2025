package com.hcen.periferico.dao;

import com.hcen.periferico.entity.Especialidad;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO para gestión de especialidades médicas.
 */
@Stateless
public class EspecialidadDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    /**
     * Guarda o actualiza una especialidad
     */
    public Especialidad save(Especialidad especialidad) {
        return em.merge(especialidad);
    }

    /**
     * Busca una especialidad por ID
     */
    public Optional<Especialidad> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        Especialidad especialidad = em.find(Especialidad.class, id);
        return Optional.ofNullable(especialidad);
    }

    /**
     * Busca una especialidad por nombre exacto
     */
    public Optional<Especialidad> findByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return Optional.empty();
        }
        TypedQuery<Especialidad> query = em.createQuery(
            "SELECT e FROM Especialidad e WHERE e.nombre = :nombre",
            Especialidad.class
        );
        query.setParameter("nombre", nombre.trim());
        List<Especialidad> result = query.setMaxResults(1).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Lista todas las especialidades ordenadas alfabéticamente
     */
    public List<Especialidad> findAll() {
        TypedQuery<Especialidad> query = em.createQuery(
            "SELECT e FROM Especialidad e ORDER BY e.nombre ASC",
            Especialidad.class
        );
        return query.getResultList();
    }

    /**
     * Busca especialidades por nombre parcial (para autocompletado)
     */
    public List<Especialidad> findByNombreContaining(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll();
        }
        TypedQuery<Especialidad> query = em.createQuery(
            "SELECT e FROM Especialidad e WHERE LOWER(e.nombre) LIKE LOWER(:term) ORDER BY e.nombre ASC",
            Especialidad.class
        );
        query.setParameter("term", "%" + searchTerm.trim() + "%");
        return query.getResultList();
    }

    /**
     * Verifica si existe una especialidad con un nombre dado
     */
    public boolean existsByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(e) FROM Especialidad e WHERE e.nombre = :nombre",
            Long.class
        );
        query.setParameter("nombre", nombre.trim());
        return query.getSingleResult() > 0;
    }

    /**
     * Cuenta el total de especialidades
     */
    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(e) FROM Especialidad e",
            Long.class
        );
        return query.getSingleResult();
    }

    /**
     * Elimina una especialidad
     * Nota: Fallará si hay profesionales asignados (FK constraint)
     */
    public void delete(Especialidad especialidad) {
        if (!em.contains(especialidad)) {
            especialidad = em.merge(especialidad);
        }
        em.remove(especialidad);
    }

    /**
     * Elimina una especialidad por ID
     */
    public boolean deleteById(UUID id) {
        Optional<Especialidad> especialidad = findById(id);
        if (especialidad.isPresent()) {
            delete(especialidad.get());
            return true;
        }
        return false;
    }
}
