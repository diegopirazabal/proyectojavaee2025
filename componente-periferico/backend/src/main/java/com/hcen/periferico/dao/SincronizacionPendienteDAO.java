package com.hcen.periferico.dao;

import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.SincronizacionPendiente.EstadoSincronizacion;
import com.hcen.periferico.enums.TipoSincronizacion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO para gestionar sincronizaciones pendientes (Dead Letter Queue local)
 */
@Stateless
public class SincronizacionPendienteDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    /**
     * Persiste o actualiza una sincronización pendiente
     */
    public SincronizacionPendiente save(SincronizacionPendiente sincronizacion) {
        return em.merge(sincronizacion);
    }

    /**
     * Busca una sincronización pendiente por usuario
     */
    public Optional<SincronizacionPendiente> findByUsuarioCedulaAndTenant(String cedula, UUID tenantId) {
        TypedQuery<SincronizacionPendiente> query = em.createQuery(
            "SELECT s FROM SincronizacionPendiente s " +
            "WHERE s.usuarioCedula = :cedula AND s.tenantId = :tenantId " +
            "AND s.estado IN ('PENDIENTE', 'ERROR') " +
            "ORDER BY s.createdAt DESC",
            SincronizacionPendiente.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", tenantId);
        query.setMaxResults(1);

        List<SincronizacionPendiente> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Obtiene todas las sincronizaciones pendientes
     */
    public List<SincronizacionPendiente> findAllPendientes() {
        return findByEstado(EstadoSincronizacion.PENDIENTE);
    }

    /**
     * Obtiene sincronizaciones por estado
     */
    public List<SincronizacionPendiente> findByEstado(EstadoSincronizacion estado) {
        TypedQuery<SincronizacionPendiente> query = em.createQuery(
            "SELECT s FROM SincronizacionPendiente s WHERE s.estado = :estado ORDER BY s.createdAt",
            SincronizacionPendiente.class
        );
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    /**
     * Obtiene sincronizaciones con errores (para reintentos)
     */
    public List<SincronizacionPendiente> findConErrores() {
        return findByEstado(EstadoSincronizacion.ERROR);
    }

    /**
     * Obtiene sincronizaciones con errores que tienen menos de N intentos
     */
    public List<SincronizacionPendiente> findParaReintentar(int maxIntentos) {
        TypedQuery<SincronizacionPendiente> query = em.createQuery(
            "SELECT s FROM SincronizacionPendiente s " +
            "WHERE s.estado IN ('PENDIENTE', 'ERROR') AND s.intentos < :maxIntentos " +
            "ORDER BY s.createdAt",
            SincronizacionPendiente.class
        );
        query.setParameter("maxIntentos", maxIntentos);
        return query.getResultList();
    }

    /**
     * Obtiene sincronizaciones de un tipo específico que tienen menos de N intentos
     * Útil para separar el procesamiento de usuarios y documentos
     */
    public List<SincronizacionPendiente> findByTipoParaReintentar(TipoSincronizacion tipo, int maxIntentos) {
        TypedQuery<SincronizacionPendiente> query = em.createQuery(
            "SELECT s FROM SincronizacionPendiente s " +
            "WHERE s.tipo = :tipo AND s.estado IN ('PENDIENTE', 'ERROR') AND s.intentos < :maxIntentos " +
            "ORDER BY s.createdAt",
            SincronizacionPendiente.class
        );
        query.setParameter("tipo", tipo);
        query.setParameter("maxIntentos", maxIntentos);
        return query.getResultList();
    }

    /**
     * Elimina una sincronización pendiente
     */
    public void delete(SincronizacionPendiente sincronizacion) {
        if (!em.contains(sincronizacion)) {
            sincronizacion = em.merge(sincronizacion);
        }
        em.remove(sincronizacion);
    }

    /**
     * Elimina sincronizaciones resueltas más antiguas que N días
     */
    public int deleteResueltasAntiguas(int dias) {
        return em.createQuery(
            "DELETE FROM SincronizacionPendiente s " +
            "WHERE s.estado = 'RESUELTA' AND s.updatedAt < CURRENT_TIMESTAMP - :dias DAY"
        )
        .setParameter("dias", dias)
        .executeUpdate();
    }

    /**
     * Cuenta sincronizaciones pendientes por tenant
     */
    public long countByTenantIdAndEstado(UUID tenantId, EstadoSincronizacion estado) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(s) FROM SincronizacionPendiente s " +
            "WHERE s.tenantId = :tenantId AND s.estado = :estado",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        query.setParameter("estado", estado);
        return query.getSingleResult();
    }
}
