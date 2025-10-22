package com.hcen.periferico.dao;

import com.hcen.core.domain.clinica;
import com.hcen.core.domain.usuario_salud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class UsuarioSaludDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public usuario_salud save(usuario_salud usuario) {
        if (usuario.getCi() == null || !em.contains(usuario)) {
            return em.merge(usuario);
        } else {
            return em.merge(usuario);
        }
    }

    public Optional<usuario_salud> findByCi(Integer ci) {
        usuario_salud usuario = em.find(usuario_salud.class, ci);
        return Optional.ofNullable(usuario);
    }

    public List<usuario_salud> findAll() {
        TypedQuery<usuario_salud> query = em.createQuery(
            "SELECT u FROM usuario_salud u ORDER BY u.apellidos, u.nombre",
            usuario_salud.class
        );
        return query.getResultList();
    }

    public boolean existsByCi(Integer ci) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM usuario_salud u WHERE u.ci = :ci",
            Long.class
        );
        query.setParameter("ci", ci);
        return query.getSingleResult() > 0;
    }

    public void delete(usuario_salud usuario) {
        if (!em.contains(usuario)) {
            usuario = em.merge(usuario);
        }
        em.remove(usuario);
    }

    public void deleteByCi(Integer ci) {
        findByCi(ci).ifPresent(this::delete);
    }

    public List<usuario_salud> findByNombreOrApellido(String searchTerm) {
        TypedQuery<usuario_salud> query = em.createQuery(
            "SELECT u FROM usuario_salud u WHERE " +
            "LOWER(u.nombre) LIKE LOWER(:term) OR LOWER(u.apellidos) LIKE LOWER(:term) " +
            "ORDER BY u.apellidos, u.nombre",
            usuario_salud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getResultList();
    }

    public List<usuario_salud> findAllPaginated(int page, int size) {
        TypedQuery<usuario_salud> query = em.createQuery(
            "SELECT u FROM usuario_salud u ORDER BY u.apellidos, u.nombre",
            usuario_salud.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<usuario_salud> findByNombreOrApellidoPaginated(String searchTerm, int page, int size) {
        TypedQuery<usuario_salud> query = em.createQuery(
            "SELECT u FROM usuario_salud u WHERE " +
            "LOWER(u.nombre) LIKE LOWER(:term) OR LOWER(u.apellidos) LIKE LOWER(:term) " +
            "ORDER BY u.apellidos, u.nombre",
            usuario_salud.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM usuario_salud u",
            Long.class
        );
        return query.getSingleResult();
    }

    public long countByNombreOrApellido(String searchTerm) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM usuario_salud u WHERE " +
            "LOWER(u.nombre) LIKE LOWER(:term) OR LOWER(u.apellidos) LIKE LOWER(:term)",
            Long.class
        );
        query.setParameter("term", "%" + searchTerm + "%");
        return query.getSingleResult();
    }

    /**
     * Asocia un usuario a una clínica (relación ManyToMany)
     */
    public void associateUsuarioToClinica(Integer usuarioCi, UUID clinicaId) {
        usuario_salud usuario = em.find(usuario_salud.class, usuarioCi);
        clinica clinicaEntity = em.find(clinica.class, clinicaId);

        if (usuario != null && clinicaEntity != null) {
            // Agregar la clínica al Set de clínicas del usuario
            usuario.getClinicas().add(clinicaEntity);
            em.merge(usuario);
        }
    }

    /**
     * Verifica si un usuario ya está asociado a una clínica
     */
    public boolean isUsuarioAssociatedToClinica(Integer usuarioCi, UUID clinicaId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM usuario_salud u JOIN u.clinicas c " +
            "WHERE u.ci = :usuarioCi AND c.id = :clinicaId",
            Long.class
        );
        query.setParameter("usuarioCi", usuarioCi);
        query.setParameter("clinicaId", clinicaId);
        return query.getSingleResult() > 0;
    }

    /**
     * Obtiene usuarios asociados a una clínica específica
     */
    public List<usuario_salud> findUsuariosByClinica(UUID clinicaId, int page, int size) {
        TypedQuery<usuario_salud> query = em.createQuery(
            "SELECT u FROM usuario_salud u JOIN u.clinicas c " +
            "WHERE c.id = :clinicaId ORDER BY u.apellidos, u.nombre",
            usuario_salud.class
        );
        query.setParameter("clinicaId", clinicaId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Cuenta usuarios asociados a una clínica específica
     */
    public long countUsuariosByClinica(UUID clinicaId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM usuario_salud u JOIN u.clinicas c " +
            "WHERE c.id = :clinicaId",
            Long.class
        );
        query.setParameter("clinicaId", clinicaId);
        return query.getSingleResult();
    }
}
