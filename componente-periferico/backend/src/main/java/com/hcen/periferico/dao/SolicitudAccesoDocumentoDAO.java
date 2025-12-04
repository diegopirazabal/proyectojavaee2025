package com.hcen.periferico.dao;

import com.hcen.periferico.entity.solicitud_acceso_documento;
import com.hcen.periferico.entity.solicitud_acceso_documento.EstadoSolicitud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DAO para gestión de solicitudes de acceso a documentos clínicos.
 * Permite controlar el anti-spam de solicitudes (máximo 1 por minuto).
 */
@Stateless
public class SolicitudAccesoDocumentoDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    /**
     * Persiste o actualiza una solicitud de acceso
     */
    public solicitud_acceso_documento save(solicitud_acceso_documento solicitud) {
        return em.merge(solicitud);
    }

    /**
     * Busca una solicitud por ID
     */
    public Optional<solicitud_acceso_documento> findById(UUID id) {
        try {
            solicitud_acceso_documento solicitud = em.find(solicitud_acceso_documento.class, id);
            return Optional.ofNullable(solicitud);
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    /**
     * Encuentra la última solicitud de un profesional para un documento específico
     * Usado para controlar que no se envíen solicitudes duplicadas antes de un minuto
     */
    public Optional<solicitud_acceso_documento> findUltimaSolicitud(
            UUID documentoId,
            Integer profesionalCi,
            UUID tenantId) {
        try {
            TypedQuery<solicitud_acceso_documento> query = em.createQuery(
                "SELECT s FROM solicitud_acceso_documento s " +
                "WHERE s.documentoId = :documentoId " +
                "  AND s.profesionalCi = :profesionalCi " +
                "  AND s.tenantId = :tenantId " +
                "ORDER BY s.fechaSolicitud DESC",
                solicitud_acceso_documento.class
            );
            query.setParameter("documentoId", documentoId);
            query.setParameter("profesionalCi", profesionalCi);
            query.setParameter("tenantId", tenantId);
            query.setMaxResults(1);

            List<solicitud_acceso_documento> resultados = query.getResultList();
            return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));

        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    /**
     * Verifica si un profesional puede volver a solicitar acceso a un documento
     * Retorna true si:
     * - Nunca solicitó acceso antes
     * - La última solicitud fue hace más de un minuto
     * - La última solicitud fue rechazada hace más de un minuto
     */
    public boolean puedeVolverASolicitar(UUID documentoId, Integer profesionalCi, UUID tenantId) {
        Optional<solicitud_acceso_documento> ultimaSolicitud =
            findUltimaSolicitud(documentoId, profesionalCi, tenantId);

        if (ultimaSolicitud.isEmpty()) {
            return true; // Nunca solicitó
        }

        solicitud_acceso_documento solicitud = ultimaSolicitud.get();

        // Si la solicitud fue aprobada, no puede volver a solicitar
        // (ya tiene permiso otorgado por el paciente)
        if (solicitud.getEstado() == EstadoSolicitud.APROBADA) {
            return false;
        }

        // Para solicitudes pendientes o rechazadas, verificar el intervalo mínimo
        return solicitud.puedeVolverASolicitar();
    }

    /**
     * Registra una nueva solicitud de acceso
     * IMPORTANTE: Solo llamar después de verificar con puedeVolverASolicitar()
     */
    public solicitud_acceso_documento registrarSolicitud(
            UUID documentoId,
            Integer profesionalCi,
            UUID tenantId,
            String cedulaPaciente) {

        solicitud_acceso_documento solicitud = new solicitud_acceso_documento();
        solicitud.setDocumentoId(documentoId);
        solicitud.setProfesionalCi(profesionalCi);
        solicitud.setTenantId(tenantId);
        solicitud.setCedulaPaciente(cedulaPaciente);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        return save(solicitud);
    }

    /**
     * Lista todas las solicitudes pendientes de un paciente
     * Útil para mostrar en la app mobile
     */
    public List<solicitud_acceso_documento> listarSolicitudesPendientesPorPaciente(String cedulaPaciente) {
        TypedQuery<solicitud_acceso_documento> query = em.createQuery(
            "SELECT s FROM solicitud_acceso_documento s " +
            "WHERE s.cedulaPaciente = :cedula " +
            "  AND s.estado = :estado " +
            "ORDER BY s.fechaSolicitud DESC",
            solicitud_acceso_documento.class
        );
        query.setParameter("cedula", cedulaPaciente);
        query.setParameter("estado", EstadoSolicitud.PENDIENTE);
        return query.getResultList();
    }

    /**
     * Lista todas las solicitudes de un documento específico
     */
    public List<solicitud_acceso_documento> listarSolicitudesPorDocumento(UUID documentoId) {
        TypedQuery<solicitud_acceso_documento> query = em.createQuery(
            "SELECT s FROM solicitud_acceso_documento s " +
            "WHERE s.documentoId = :documentoId " +
            "ORDER BY s.fechaSolicitud DESC",
            solicitud_acceso_documento.class
        );
        query.setParameter("documentoId", documentoId);
        return query.getResultList();
    }

    /**
     * Cuenta la cantidad de solicitudes pendientes de un paciente
     */
    public long contarSolicitudesPendientes(String cedulaPaciente) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(s) FROM solicitud_acceso_documento s " +
            "WHERE s.cedulaPaciente = :cedula " +
            "  AND s.estado = :estado",
            Long.class
        );
        query.setParameter("cedula", cedulaPaciente);
        query.setParameter("estado", EstadoSolicitud.PENDIENTE);
        return query.getSingleResult();
    }

    /**
     * Marca una solicitud como aprobada
     * Se llama cuando el paciente otorga el permiso desde la app mobile
     */
    public void aprobarSolicitud(UUID solicitudId) {
        solicitud_acceso_documento solicitud = em.find(solicitud_acceso_documento.class, solicitudId);
        if (solicitud != null) {
            solicitud.aprobar();
            save(solicitud);
        }
    }

    /**
     * Marca una solicitud como rechazada
     */
    public void rechazarSolicitud(UUID solicitudId) {
        solicitud_acceso_documento solicitud = em.find(solicitud_acceso_documento.class, solicitudId);
        if (solicitud != null) {
            solicitud.rechazar();
            save(solicitud);
        }
    }

    /**
     * Cuenta solicitudes por tenant y estado
     */
    public long countByTenantIdAndEstado(UUID tenantId, EstadoSolicitud estado) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(s) FROM solicitud_acceso_documento s " +
            "WHERE s.tenantId = :tenantId AND s.estado = :estado",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        query.setParameter("estado", estado);
        return query.getSingleResult();
    }

    /**
     * Cuenta solicitudes por estado (todas las clínicas)
     */
    public long countByEstado(EstadoSolicitud estado) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(s) FROM solicitud_acceso_documento s WHERE s.estado = :estado",
            Long.class
        );
        query.setParameter("estado", estado);
        return query.getSingleResult();
    }

    /**
     * Cuenta solicitudes de acceso agrupadas por tenant_id y filtradas por estado
     * Optimizado para evitar N+1 queries en reportes
     *
     * @param tenantIds Colección de tenant IDs a consultar
     * @param estado Estado de las solicitudes (ej: APROBADA)
     * @return Map con tenantId -> cantidad de solicitudes en ese estado
     */
    public Map<UUID, Long> countByTenantIdAndEstadoBatch(
            Collection<UUID> tenantIds,
            EstadoSolicitud estado) {
        Map<UUID, Long> resultado = new HashMap<>();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return resultado;
        }

        try {
            List<UUID> ids = tenantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            if (ids.isEmpty()) {
                return resultado;
            }

            TypedQuery<Object[]> query = em.createQuery(
                "SELECT s.tenantId, COUNT(s) FROM solicitud_acceso_documento s " +
                "WHERE s.tenantId IN :tenantIds AND s.estado = :estado " +
                "GROUP BY s.tenantId",
                Object[].class
            );
            query.setParameter("tenantIds", ids);
            query.setParameter("estado", estado);
            List<Object[]> results = query.getResultList();

            for (Object[] row : results) {
                UUID tenantId = (UUID) row[0];
                Long count = (Long) row[1];
                resultado.put(tenantId, count);
            }
        } catch (Exception e) {
            // Log error
            e.printStackTrace();
        }
        return resultado;
    }
}
