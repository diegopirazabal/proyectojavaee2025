package hcen.central.inus.dao;

import hcen.central.inus.entity.OIDCSession;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestión de sesiones OIDC
 */
@Stateless
public class OIDCSessionDAO {
    
    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;
    
    /**
     * Crea una nueva sesión
     */
    public OIDCSession createSession(OIDCSession session) {
        em.persist(session);
        return session;
    }
    
    /**
     * Guarda o actualiza una sesión
     */
    public OIDCSession save(OIDCSession session) {
        if (session.getId() == null) {
            em.persist(session);
            return session;
        } else {
            return em.merge(session);
        }
    }
    
    /**
     * Busca sesión por sessionId
     */
    public Optional<OIDCSession> findBySessionId(String sessionId) {
        try {
            TypedQuery<OIDCSession> query = em.createQuery(
                "SELECT s FROM OIDCSession s WHERE s.sessionId = :sessionId AND s.active = true",
                OIDCSession.class
            );
            query.setParameter("sessionId", sessionId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca todas las sesiones activas de un usuario
     */
    public List<OIDCSession> findByUserSub(String userSub) {
        TypedQuery<OIDCSession> query = em.createQuery(
            "SELECT s FROM OIDCSession s WHERE s.userSub = :userSub AND s.active = true ORDER BY s.createdAt DESC",
            OIDCSession.class
        );
        query.setParameter("userSub", userSub);
        return query.getResultList();
    }
    
    /**
     * Actualiza los tokens de una sesión
     */
    public void updateTokens(String sessionId, String accessToken, String refreshToken, String idToken) {
        Optional<OIDCSession> sessionOpt = findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            OIDCSession session = sessionOpt.get();
            session.setAccessToken(accessToken);
            session.setRefreshToken(refreshToken);
            session.setIdToken(idToken);
            em.merge(session);
        }
    }
    
    /**
     * Actualiza la fecha de expiración de una sesión
     */
    public void updateExpiration(String sessionId, Instant expiresAt) {
        Optional<OIDCSession> sessionOpt = findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            OIDCSession session = sessionOpt.get();
            session.setExpiresAt(expiresAt);
            em.merge(session);
        }
    }
    
    /**
     * Elimina una sesión (marca como inactiva)
     */
    public void deleteSession(String sessionId) {
        Optional<OIDCSession> sessionOpt = findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            OIDCSession session = sessionOpt.get();
            session.setActive(false);
            em.merge(session);
        }
    }
    
    /**
     * Elimina permanentemente una sesión (hard delete)
     */
    public void hardDeleteSession(String sessionId) {
        TypedQuery<OIDCSession> query = em.createQuery(
            "SELECT s FROM OIDCSession s WHERE s.sessionId = :sessionId",
            OIDCSession.class
        );
        query.setParameter("sessionId", sessionId);
        try {
            OIDCSession session = query.getSingleResult();
            em.remove(session);
        } catch (NoResultException e) {
            // Sesión no existe
        }
    }
    
    /**
     * Elimina todas las sesiones de un usuario
     */
    public void deleteAllUserSessions(String userSub) {
        List<OIDCSession> sessions = findByUserSub(userSub);
        for (OIDCSession session : sessions) {
            session.setActive(false);
            em.merge(session);
        }
    }
    
    /**
     * Elimina todas las sesiones de un usuario (alias para deleteAllUserSessions)
     */
    public void deleteAllSessionsByUserSub(String userSub) {
        deleteAllUserSessions(userSub);
    }
    
    /**
     * Elimina sesiones expiradas
     * @return Número de sesiones eliminadas
     */
    public int deleteExpiredSessions() {
        Instant now = Instant.now();
        TypedQuery<OIDCSession> query = em.createQuery(
            "SELECT s FROM OIDCSession s WHERE s.expiresAt < :now AND s.active = true",
            OIDCSession.class
        );
        query.setParameter("now", now);
        
        List<OIDCSession> expiredSessions = query.getResultList();
        for (OIDCSession session : expiredSessions) {
            session.setActive(false);
            em.merge(session);
        }
        
        return expiredSessions.size();
    }
    
    /**
     * Limpia sesiones antiguas inactivas (hard delete)
     * @param daysOld Eliminar sesiones inactivas más antiguas que X días
     * @return Número de sesiones eliminadas
     */
    public int cleanupOldSessions(int daysOld) {
        Instant cutoffDate = Instant.now().minus(Duration.ofDays(daysOld));
        TypedQuery<OIDCSession> query = em.createQuery(
            "SELECT s FROM OIDCSession s WHERE s.active = false AND s.updatedAt < :cutoff",
            OIDCSession.class
        );
        query.setParameter("cutoff", cutoffDate);
        
        List<OIDCSession> oldSessions = query.getResultList();
        for (OIDCSession session : oldSessions) {
            em.remove(session);
        }
        
        return oldSessions.size();
    }
    
    /**
     * Cuenta sesiones activas de un usuario
     */
    public long countActiveSessions(String userSub) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(s) FROM OIDCSession s WHERE s.userSub = :userSub AND s.active = true",
            Long.class
        );
        query.setParameter("userSub", userSub);
        return query.getSingleResult();
    }
    
    /**
     * Verifica si existe una sesión activa
     */
    public boolean isSessionActive(String sessionId) {
        return findBySessionId(sessionId).isPresent();
    }
}
