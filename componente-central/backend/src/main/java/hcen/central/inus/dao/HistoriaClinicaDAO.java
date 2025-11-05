package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.historia_clinica_documento;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class HistoriaClinicaDAO {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    public historia_clinica save(historia_clinica historia) {
        if (historia.getId() == null) {
            em.persist(historia);
            return historia;
        }
        return em.merge(historia);
    }

    public Optional<historia_clinica> findByUsuario(UsuarioSalud usuario) {
        TypedQuery<historia_clinica> query = em.createQuery(
            "SELECT h FROM historia_clinica h WHERE h.usuario = :usuario",
            historia_clinica.class
        );
        query.setParameter("usuario", usuario);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    public Optional<historia_clinica> findByCedulaAndTenant(String cedula, UUID tenantId) {
        TypedQuery<historia_clinica> query = em.createQuery(
            "SELECT h FROM historia_clinica h JOIN h.usuario u " +
                "WHERE u.cedula = :cedula AND u.tenantId = :tenantId",
            historia_clinica.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", tenantId);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    public historia_clinica_documento saveDocumento(historia_clinica_documento doc) {
        if (doc.getId() == null) {
            em.persist(doc);
            return doc;
        }
        return em.merge(doc);
    }

    public boolean existsDocumento(UUID historiaId, UUID documentoId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(d) FROM historia_clinica_documento d " +
            "WHERE d.historiaClinica.id = :historiaId AND d.documentoId = :documentoId",
            Long.class
        );
        query.setParameter("historiaId", historiaId);
        query.setParameter("documentoId", documentoId);
        return query.getSingleResult() > 0;
    }

    public List<historia_clinica_documento> findDocumentosByHistoria(UUID historiaId) {
        TypedQuery<historia_clinica_documento> query = em.createQuery(
            "SELECT d FROM historia_clinica_documento d WHERE d.historiaClinica.id = :historiaId",
            historia_clinica_documento.class
        );
        query.setParameter("historiaId", historiaId);
        return query.getResultList();
    }
}
