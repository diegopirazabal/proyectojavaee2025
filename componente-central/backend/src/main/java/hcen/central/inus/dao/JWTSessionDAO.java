package hcen.central.inus.dao;

import hcen.central.inus.entity.JWTSession;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestionar sesiones JWT de clientes externos (componente-periferico)
 */
@Stateless
public class JWTSessionDAO {
    
    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;
    
    /**
     * Guarda o actualiza una sesión JWT
     */
    public JWTSession save(JWTSession session) {
        if (session.getId() == null) {
            em.persist(session);
            return session;
        } else {
            return em.merge(session);
        }
    }
    
    /**
     * Busca una sesión por el token JWT
     */
    public Optional<JWTSession> findByToken(String token) {
        try {
            TypedQuery<JWTSession> query = em.createQuery(
                "SELECT s FROM JWTSession s WHERE s.jwtToken = :token AND s.active = true", 
                JWTSession.class
            );
            query.setParameter("token", token);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca sesiones activas por client_id
     */
    public List<JWTSession> findActiveByClientId(String clientId) {
        TypedQuery<JWTSession> query = em.createQuery(
            "SELECT s FROM JWTSession s WHERE s.clientId = :clientId AND s.active = true AND s.expiresAt > :now ORDER BY s.issuedAt DESC", 
            JWTSession.class
        );
        query.setParameter("clientId", clientId);
        query.setParameter("now", LocalDateTime.now());
        return query.getResultList();
    }
    
    /**
     * Invalida todas las sesiones de un cliente
     */
    public int invalidateAllByClientId(String clientId) {
        return em.createQuery(
            "UPDATE JWTSession s SET s.active = false WHERE s.clientId = :clientId"
        )
        .setParameter("clientId", clientId)
        .executeUpdate();
    }
    
    /**
     * Invalida una sesión específica por token
     */
    public int invalidateByToken(String token) {
        return em.createQuery(
            "UPDATE JWTSession s SET s.active = false WHERE s.jwtToken = :token"
        )
        .setParameter("token", token)
        .executeUpdate();
    }
    
    /**
     * Actualiza el timestamp de último uso de una sesión
     */
    public void updateLastUsed(Long sessionId) {
        JWTSession session = em.find(JWTSession.class, sessionId);
        if (session != null) {
            session.setLastUsedAt(LocalDateTime.now());
            em.merge(session);
        }
    }
    
    /**
     * Elimina sesiones expiradas (limpieza periódica)
     */
    public int deleteExpiredSessions() {
        return em.createQuery(
            "DELETE FROM JWTSession s WHERE s.expiresAt < :now OR s.active = false"
        )
        .setParameter("now", LocalDateTime.now().minusDays(7)) // Mantener histórico de 7 días
        .executeUpdate();
    }
    
    /**
     * Verifica si existe una sesión activa y válida para un token
     */
    public boolean isTokenValid(String token) {
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(s) FROM JWTSession s WHERE s.jwtToken = :token AND s.active = true AND s.expiresAt > :now", 
                Long.class
            );
            query.setParameter("token", token);
            query.setParameter("now", LocalDateTime.now());
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtiene todas las sesiones activas
     */
    public List<JWTSession> findAllActive() {
        TypedQuery<JWTSession> query = em.createQuery(
            "SELECT s FROM JWTSession s WHERE s.active = true AND s.expiresAt > :now ORDER BY s.issuedAt DESC", 
            JWTSession.class
        );
        query.setParameter("now", LocalDateTime.now());
        return query.getResultList();
    }
}
