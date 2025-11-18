package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.notificacion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO para gestión de notificaciones en base de datos
 */
@Stateless
public class NotificacionDAO {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    /**
     * Guarda una nueva notificación
     */
    public notificacion save(notificacion notif) {
        if (notif.getId() == null) {
            em.persist(notif);
            return notif;
        } else {
            return em.merge(notif);
        }
    }

    /**
     * Busca una notificación por ID
     */
    public Optional<notificacion> findById(UUID id) {
        notificacion notif = em.find(notificacion.class, id);
        return Optional.ofNullable(notif);
    }

    /**
     * Lista todas las notificaciones de un usuario por cédula
     */
    public List<notificacion> findByUsuarioCedula(String cedula) {
        String jpql = "SELECT n FROM notificacion n " +
                     "WHERE n.usuario.cedula = :cedula " +
                     "ORDER BY n.fecCreacion DESC";
        TypedQuery<notificacion> query = em.createQuery(jpql, notificacion.class);
        query.setParameter("cedula", cedula);
        return query.getResultList();
    }

    /**
     * Lista solicitudes de acceso pendientes de un usuario
     */
    public List<notificacion> findSolicitudesPendientesByCedula(String cedula) {
        String jpql = "SELECT n FROM notificacion n " +
                     "WHERE n.usuario.cedula = :cedula " +
                     "AND n.tipo = 'SOLICITUD_ACCESO' " +
                     "AND n.estado = 'PENDIENTE' " +
                     "ORDER BY n.fecCreacion DESC";
        TypedQuery<notificacion> query = em.createQuery(jpql, notificacion.class);
        query.setParameter("cedula", cedula);
        return query.getResultList();
    }

    /**
     * Lista notificaciones por tipo y estado
     */
    public List<notificacion> findByTipoAndEstado(String tipo, String estado) {
        String jpql = "SELECT n FROM notificacion n " +
                     "WHERE n.tipo = :tipo " +
                     "AND n.estado = :estado " +
                     "ORDER BY n.fecCreacion DESC";
        TypedQuery<notificacion> query = em.createQuery(jpql, notificacion.class);
        query.setParameter("tipo", tipo);
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    /**
     * Busca notificación por documentoId y profesionalCi en el mensaje
     * (asumiendo que esta info está en el campo mensaje como JSON)
     */
    public Optional<notificacion> findSolicitudByDocumentoAndProfesional(
            String cedula, String documentoId, Integer profesionalCi) {
        String jpql = "SELECT n FROM notificacion n " +
                     "WHERE n.usuario.cedula = :cedula " +
                     "AND n.tipo = 'SOLICITUD_ACCESO' " +
                     "AND n.mensaje LIKE :searchPattern " +
                     "ORDER BY n.fecCreacion DESC";
        TypedQuery<notificacion> query = em.createQuery(jpql, notificacion.class);
        query.setParameter("cedula", cedula);
        // Buscar documentoId y profesionalCi en el mensaje JSON
        query.setParameter("searchPattern", "%\"documentoId\":\"" + documentoId + "\"%\"profesionalCi\":" + profesionalCi + "%");
        query.setMaxResults(1);

        List<notificacion> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Actualiza el estado de una notificación
     */
    public void actualizarEstado(UUID id, String nuevoEstado) {
        Optional<notificacion> notifOpt = findById(id);
        if (notifOpt.isPresent()) {
            notificacion notif = notifOpt.get();
            notif.setEstado(nuevoEstado);
            em.merge(notif);
        }
    }

    /**
     * Cuenta notificaciones pendientes por usuario
     */
    public long countPendientesByUsuario(String cedula) {
        String jpql = "SELECT COUNT(n) FROM notificacion n " +
                     "WHERE n.usuario.cedula = :cedula " +
                     "AND n.estado = 'PENDIENTE'";
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        query.setParameter("cedula", cedula);
        return query.getSingleResult();
    }
}
