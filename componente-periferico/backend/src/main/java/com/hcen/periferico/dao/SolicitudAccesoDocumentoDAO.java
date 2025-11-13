package com.hcen.periferico.dao;

import com.hcen.periferico.entity.solicitud_acceso_documento;
import com.hcen.periferico.entity.solicitud_acceso_documento.EstadoSolicitud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO para gestión de solicitudes de acceso a documentos clínicos.
 * Permite controlar el anti-spam de solicitudes (máximo 1 cada 24 horas).
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
     * Usado para controlar que no se envíen solicitudes duplicadas antes de 24 horas
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
     * - La última solicitud fue hace más de 24 horas
     * - La última solicitud fue rechazada hace más de 24 horas
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

        // Para solicitudes pendientes o rechazadas, verificar 24 horas
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
}
