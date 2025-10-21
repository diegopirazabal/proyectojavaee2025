package hcen.central.notifications.dao;

import hcen.central.notifications.entity.FCMToken;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestionar tokens FCM en la base de datos
 */
@Stateless
public class fcm_token_dao {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    /**
     * Guardar un nuevo token FCM
     */
    public FCMToken save(FCMToken token) {
        em.persist(token);
        em.flush();
        return token;
    }

    /**
     * Actualizar un token FCM existente
     */
    public FCMToken update(FCMToken token) {
        return em.merge(token);
    }

    /**
     * Buscar token por ID
     */
    public Optional<FCMToken> findById(Long id) {
        FCMToken token = em.find(FCMToken.class, id);
        return Optional.ofNullable(token);
    }

    /**
     * Buscar token por el FCM token string
     */
    public Optional<FCMToken> findByFcmToken(String fcmToken) {
        try {
            TypedQuery<FCMToken> query = em.createQuery(
                "SELECT t FROM FCMToken t WHERE t.fcmToken = :fcmToken",
                FCMToken.class
            );
            query.setParameter("fcmToken", fcmToken);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Buscar token por usuario y deviceId
     */
    public Optional<FCMToken> findByUsuarioAndDevice(Long usuarioId, String deviceId) {
        try {
            TypedQuery<FCMToken> query = em.createQuery(
                "SELECT t FROM FCMToken t WHERE t.usuarioId = :usuarioId AND t.deviceId = :deviceId",
                FCMToken.class
            );
            query.setParameter("usuarioId", usuarioId);
            query.setParameter("deviceId", deviceId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Obtener todos los tokens activos de un usuario
     */
    public List<FCMToken> findActiveTokensByUsuarioId(Long usuarioId) {
        TypedQuery<FCMToken> query = em.createQuery(
            "SELECT t FROM FCMToken t WHERE t.usuarioId = :usuarioId AND t.active = true",
            FCMToken.class
        );
        query.setParameter("usuarioId", usuarioId);
        return query.getResultList();
    }

    /**
     * Obtener todos los tokens de un usuario (activos e inactivos)
     */
    public List<FCMToken> findAllTokensByUsuarioId(Long usuarioId) {
        TypedQuery<FCMToken> query = em.createQuery(
            "SELECT t FROM FCMToken t WHERE t.usuarioId = :usuarioId",
            FCMToken.class
        );
        query.setParameter("usuarioId", usuarioId);
        return query.getResultList();
    }

    /**
     * Desactivar un token (soft delete)
     */
    public void deactivate(FCMToken token) {
        token.setActive(false);
        em.merge(token);
    }

    /**
     * Desactivar token por FCM token string
     */
    public boolean deactivateByFcmToken(String fcmToken) {
        Optional<FCMToken> tokenOpt = findByFcmToken(fcmToken);
        if (tokenOpt.isPresent()) {
            deactivate(tokenOpt.get());
            return true;
        }
        return false;
    }

    /**
     * Eliminar token permanentemente
     */
    public void delete(FCMToken token) {
        if (!em.contains(token)) {
            token = em.merge(token);
        }
        em.remove(token);
    }

    /**
     * Eliminar token por FCM token string
     */
    public boolean deleteByFcmToken(String fcmToken) {
        Optional<FCMToken> tokenOpt = findByFcmToken(fcmToken);
        if (tokenOpt.isPresent()) {
            delete(tokenOpt.get());
            return true;
        }
        return false;
    }

    /**
     * Desactivar todos los tokens de un usuario
     */
    public int deactivateAllByUsuarioId(Long usuarioId) {
        return em.createQuery(
            "UPDATE FCMToken t SET t.active = false WHERE t.usuarioId = :usuarioId"
        )
        .setParameter("usuarioId", usuarioId)
        .executeUpdate();
    }

    /**
     * Contar tokens activos de un usuario
     */
    public long countActiveTokensByUsuarioId(Long usuarioId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM FCMToken t WHERE t.usuarioId = :usuarioId AND t.active = true",
            Long.class
        );
        query.setParameter("usuarioId", usuarioId);
        return query.getSingleResult();
    }
}
