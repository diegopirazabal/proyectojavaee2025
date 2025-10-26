package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioClinica;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

/**
 * DAO para gestionar la relación entre usuarios y clínicas
 */
@Stateless
public class UsuarioClinicaDAO {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    /**
     * Guarda una nueva asociación usuario-clínica
     */
    public UsuarioClinica save(UsuarioClinica usuarioClinica) {
        if (usuarioClinica.getId() == null) {
            em.persist(usuarioClinica);
            return usuarioClinica;
        } else {
            return em.merge(usuarioClinica);
        }
    }

    /**
     * Busca una asociación específica por usuario y clínica
     */
    public Optional<UsuarioClinica> findByUsuarioCedulaAndTenantId(String usuarioCedula, java.util.UUID tenantId) {
        try {
            TypedQuery<UsuarioClinica> query = em.createQuery(
                "SELECT uc FROM UsuarioClinica uc WHERE uc.usuarioCedula = :cedula AND uc.tenantId = :tenantId",
                UsuarioClinica.class
            );
            query.setParameter("cedula", usuarioCedula);
            query.setParameter("tenantId", tenantId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Verifica si existe una asociación activa entre usuario y clínica
     */
    public boolean existsAssociation(String usuarioCedula, java.util.UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(uc) FROM UsuarioClinica uc " +
            "WHERE uc.usuarioCedula = :cedula AND uc.tenantId = :tenantId AND uc.active = true",
            Long.class
        );
        query.setParameter("cedula", usuarioCedula);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult() > 0;
    }

    /**
     * Lista todos los usuarios asociados a una clínica (activos)
     */
    public List<UsuarioClinica> findByTenantId(java.util.UUID tenantId) {
        TypedQuery<UsuarioClinica> query = em.createQuery(
            "SELECT uc FROM UsuarioClinica uc " +
            "WHERE uc.tenantId = :tenantId AND uc.active = true " +
            "ORDER BY uc.fechaAsociacion DESC",
            UsuarioClinica.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Lista usuarios de una clínica con paginación
     */
    public List<UsuarioClinica> findByTenantIdPaginated(java.util.UUID tenantId, int page, int size) {
        TypedQuery<UsuarioClinica> query = em.createQuery(
            "SELECT uc FROM UsuarioClinica uc " +
            "WHERE uc.tenantId = :tenantId AND uc.active = true " +
            "ORDER BY uc.fechaAsociacion DESC",
            UsuarioClinica.class
        );
        query.setParameter("tenantId", tenantId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Cuenta total de usuarios asociados a una clínica
     */
    public long countByTenantId(java.util.UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(uc) FROM UsuarioClinica uc " +
            "WHERE uc.tenantId = :tenantId AND uc.active = true",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult();
    }

    /**
     * Lista todas las clínicas donde está registrado un usuario
     */
    public List<UsuarioClinica> findByUsuarioCedula(String usuarioCedula) {
        TypedQuery<UsuarioClinica> query = em.createQuery(
            "SELECT uc FROM UsuarioClinica uc " +
            "WHERE uc.usuarioCedula = :cedula AND uc.active = true " +
            "ORDER BY uc.fechaAsociacion DESC",
            UsuarioClinica.class
        );
        query.setParameter("cedula", usuarioCedula);
        return query.getResultList();
    }

    /**
     * Desactiva la asociación entre un usuario y una clínica (soft delete)
     */
    public void deactivateAssociation(String usuarioCedula, java.util.UUID tenantId) {
        Optional<UsuarioClinica> association = findByUsuarioCedulaAndTenantId(usuarioCedula, tenantId);
        association.ifPresent(uc -> {
            uc.setActive(false);
            em.merge(uc);
        });
    }

    /**
     * Elimina físicamente una asociación
     */
    public void delete(Long id) {
        UsuarioClinica association = em.find(UsuarioClinica.class, id);
        if (association != null) {
            em.remove(association);
        }
    }

    /**
     * Elimina físicamente la asociación entre un usuario y una clínica
     */
    public boolean deleteByUsuarioCedulaAndTenantId(String usuarioCedula, java.util.UUID tenantId) {
        Optional<UsuarioClinica> association = findByUsuarioCedulaAndTenantId(usuarioCedula, tenantId);
        if (association.isPresent()) {
            em.remove(association.get());
            return true;
        }
        return false;
    }
}
