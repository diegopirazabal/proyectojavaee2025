package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioClinica;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO para gestionar las asociaciones entre usuarios y clínicas.
 */
@Stateless
public class UsuarioClinicaDAO {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    public UsuarioClinica save(UsuarioClinica usuarioClinica) {
        if (usuarioClinica.getId() == null) {
            em.persist(usuarioClinica);
            return usuarioClinica;
        }
        return em.merge(usuarioClinica);
    }

    public Optional<UsuarioClinica> findByUsuarioCedulaAndTenantId(String usuarioCedula, UUID tenantId) {
        try {
            TypedQuery<UsuarioClinica> query = em.createQuery(
                "SELECT uc FROM UsuarioClinica uc WHERE uc.usuarioCedula = :cedula AND uc.tenantId = :tenantId",
                UsuarioClinica.class
            );
            query.setParameter("cedula", usuarioCedula);
            query.setParameter("tenantId", normalizeTenantId(tenantId));
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsAssociation(String usuarioCedula, UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(uc) FROM UsuarioClinica uc " +
                "WHERE uc.usuarioCedula = :cedula AND uc.tenantId = :tenantId AND uc.active = true",
            Long.class
        );
        query.setParameter("cedula", usuarioCedula);
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        return query.getSingleResult() > 0;
    }

    public List<UsuarioClinica> findByTenantId(UUID tenantId) {
        TypedQuery<UsuarioClinica> query = em.createQuery(
            "SELECT uc FROM UsuarioClinica uc " +
                "WHERE uc.tenantId = :tenantId AND uc.active = true " +
                "ORDER BY uc.fechaAsociacion DESC",
            UsuarioClinica.class
        );
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        return query.getResultList();
    }

    public List<UsuarioClinica> findByTenantIdPaginated(UUID tenantId, int page, int size) {
        TypedQuery<UsuarioClinica> query = em.createQuery(
            "SELECT uc FROM UsuarioClinica uc " +
                "WHERE uc.tenantId = :tenantId AND uc.active = true " +
                "ORDER BY uc.fechaAsociacion DESC",
            UsuarioClinica.class
        );
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public long countByTenantId(UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(uc) FROM UsuarioClinica uc " +
                "WHERE uc.tenantId = :tenantId AND uc.active = true",
            Long.class
        );
        query.setParameter("tenantId", normalizeTenantId(tenantId));
        return query.getSingleResult();
    }

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

    public void deactivateAssociation(String usuarioCedula, UUID tenantId) {
        Optional<UsuarioClinica> association = findByUsuarioCedulaAndTenantId(usuarioCedula, tenantId);
        association.ifPresent(uc -> {
            uc.setActive(false);
            em.merge(uc);
        });
    }

    public void delete(Long id) {
        UsuarioClinica association = em.find(UsuarioClinica.class, id);
        if (association != null) {
            em.remove(association);
        }
    }

    public boolean deleteByUsuarioCedulaAndTenantId(String usuarioCedula, UUID tenantId) {
        Optional<UsuarioClinica> association = findByUsuarioCedulaAndTenantId(usuarioCedula, tenantId);
        if (association.isPresent()) {
            em.remove(association.get());
            return true;
        }
        return false;
    }

    private String normalizeTenantId(UUID tenantId) {
        return tenantId != null ? tenantId.toString() : null;
    }
}
