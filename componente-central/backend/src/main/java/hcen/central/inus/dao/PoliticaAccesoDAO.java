package hcen.central.inus.dao;

import hcen.central.inus.entity.politica_acceso;
import hcen.central.inus.enums.EstadoPermiso;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO para gestión de políticas de acceso a documentos clínicos
 */
@Stateless
public class PoliticaAccesoDAO {

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    /**
     * Persiste o actualiza una política de acceso
     */
    public politica_acceso save(politica_acceso politica) {
        if (politica.getId() == null) {
            em.persist(politica);
            return politica;
        }
        return em.merge(politica);
    }

    /**
     * Busca una política por su ID
     */
    public Optional<politica_acceso> findById(UUID id) {
        try {
            politica_acceso politica = em.find(politica_acceso.class, id);
            return Optional.ofNullable(politica);
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    /**
     * Valida si un profesional tiene permiso para acceder a un documento
     *
     * @param documentoId UUID del documento
     * @param ciProfesional CI del profesional
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional
     * @return true si existe al menos un permiso activo, false en caso contrario
     */
    public boolean tienePermisoAcceso(UUID documentoId, Integer ciProfesional, UUID tenantId, String especialidad) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM politica_acceso p " +
            "WHERE p.documentoId = :documentoId " +
            "  AND p.tenantId = :tenantId " +
            "  AND p.estado = :estado " +
            "  AND p.fechaExpiracion > :ahora " +
            "  AND (" +
            "    (p.tipoPermiso = 'PROFESIONAL_ESPECIFICO' AND p.ciProfesional = :ciProfesional) " +
            "    OR (p.tipoPermiso = 'POR_ESPECIALIDAD' AND p.especialidad = :especialidad) " +
            "    OR (p.tipoPermiso = 'POR_CLINICA')" +
            "  )",
            Long.class
        );
        query.setParameter("documentoId", documentoId);
        query.setParameter("tenantId", tenantId);
        query.setParameter("ciProfesional", ciProfesional);
        query.setParameter("especialidad", especialidad);
        query.setParameter("estado", EstadoPermiso.ACTIVO);
        query.setParameter("ahora", LocalDateTime.now());

        return query.getSingleResult() > 0;
    }

    /**
     * Valida si un profesional tiene permiso para acceder a múltiples documentos (batch)
     *
     * @param documentoIds Lista de UUIDs de documentos
     * @param ciProfesional CI del profesional
     * @param tenantId UUID de la clínica
     * @param especialidad Especialidad del profesional
     * @return Map con documentoId como key y boolean (tiene permiso) como value
     */
    public Map<UUID, Boolean> tienePermisoAccesoBatch(List<UUID> documentoIds, Integer ciProfesional,
                                                       UUID tenantId, String especialidad) {
        if (documentoIds == null || documentoIds.isEmpty()) {
            return new HashMap<>();
        }

        // Query para obtener los documentos que SÍ tienen permiso
        TypedQuery<UUID> query = em.createQuery(
            "SELECT DISTINCT p.documentoId FROM politica_acceso p " +
            "WHERE p.documentoId IN :documentoIds " +
            "  AND p.tenantId = :tenantId " +
            "  AND p.estado = :estado " +
            "  AND p.fechaExpiracion > :ahora " +
            "  AND (" +
            "    (p.tipoPermiso = 'PROFESIONAL_ESPECIFICO' AND p.ciProfesional = :ciProfesional) " +
            "    OR (p.tipoPermiso = 'POR_ESPECIALIDAD' AND p.especialidad = :especialidad) " +
            "    OR (p.tipoPermiso = 'POR_CLINICA')" +
            "  )",
            UUID.class
        );
        query.setParameter("documentoIds", documentoIds);
        query.setParameter("tenantId", tenantId);
        query.setParameter("ciProfesional", ciProfesional);
        query.setParameter("especialidad", especialidad);
        query.setParameter("estado", EstadoPermiso.ACTIVO);
        query.setParameter("ahora", LocalDateTime.now());

        List<UUID> documentosConPermiso = query.getResultList();

        // Construir mapa con todos los documentos solicitados
        Map<UUID, Boolean> resultado = new HashMap<>();
        for (UUID docId : documentoIds) {
            resultado.put(docId, documentosConPermiso.contains(docId));
        }

        return resultado;
    }

    /**
     * Busca un permiso activo específico para un documento, profesional y clínica
     * Útil para verificar permisos antes de otorgarlos (evitar duplicados)
     */
    public Optional<politica_acceso> findPermisoActivo(UUID documentoId, Integer ciProfesional, UUID tenantId) {
        try {
            TypedQuery<politica_acceso> query = em.createQuery(
                "SELECT p FROM politica_acceso p " +
                "WHERE p.documentoId = :documentoId " +
                "  AND p.tenantId = :tenantId " +
                "  AND p.ciProfesional = :ciProfesional " +
                "  AND p.estado = :estado " +
                "  AND p.fechaExpiracion > :ahora",
                politica_acceso.class
            );
            query.setParameter("documentoId", documentoId);
            query.setParameter("tenantId", tenantId);
            query.setParameter("ciProfesional", ciProfesional);
            query.setParameter("estado", EstadoPermiso.ACTIVO);
            query.setParameter("ahora", LocalDateTime.now());

            return Optional.of(query.getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    /**
     * Lista todas las políticas de una historia clínica (activas, revocadas y expiradas)
     */
    public List<politica_acceso> listarPorHistoria(UUID historiaClinicaId) {
        TypedQuery<politica_acceso> query = em.createQuery(
            "SELECT p FROM politica_acceso p " +
            "WHERE p.historiaClinica.id = :historiaId " +
            "ORDER BY p.fechaOtorgamiento DESC",
            politica_acceso.class
        );
        query.setParameter("historiaId", historiaClinicaId);
        return query.getResultList();
    }

    /**
     * Lista solo las políticas activas de una historia clínica
     */
    public List<politica_acceso> listarActivasPorHistoria(UUID historiaClinicaId) {
        TypedQuery<politica_acceso> query = em.createQuery(
            "SELECT p FROM politica_acceso p " +
            "WHERE p.historiaClinica.id = :historiaId " +
            "  AND p.estado = :estado " +
            "  AND p.fechaExpiracion > :ahora " +
            "ORDER BY p.fechaExpiracion ASC",
            politica_acceso.class
        );
        query.setParameter("historiaId", historiaClinicaId);
        query.setParameter("estado", EstadoPermiso.ACTIVO);
        query.setParameter("ahora", LocalDateTime.now());
        return query.getResultList();
    }

    /**
     * Lista todas las políticas activas de un documento específico
     */
    public List<politica_acceso> listarActivasPorDocumento(UUID documentoId) {
        TypedQuery<politica_acceso> query = em.createQuery(
            "SELECT p FROM politica_acceso p " +
            "WHERE p.documentoId = :documentoId " +
            "  AND p.estado = :estado " +
            "  AND p.fechaExpiracion > :ahora " +
            "ORDER BY p.fechaOtorgamiento DESC",
            politica_acceso.class
        );
        query.setParameter("documentoId", documentoId);
        query.setParameter("estado", EstadoPermiso.ACTIVO);
        query.setParameter("ahora", LocalDateTime.now());
        return query.getResultList();
    }

    /**
     * Marca como expirados todos los permisos cuya fecha de expiración ya pasó
     * Usado por job/scheduler para mantenimiento
     *
     * @return Cantidad de permisos marcados como expirados
     */
    public int expirarPermisosVencidos() {
        return em.createQuery(
            "UPDATE politica_acceso p " +
            "SET p.estado = :expirado " +
            "WHERE p.estado = :activo " +
            "  AND p.fechaExpiracion <= :ahora"
        )
        .setParameter("expirado", EstadoPermiso.EXPIRADO)
        .setParameter("activo", EstadoPermiso.ACTIVO)
        .setParameter("ahora", LocalDateTime.now())
        .executeUpdate();
    }

    /**
     * Cuenta la cantidad de permisos activos para un documento
     */
    public long contarPermisosActivos(UUID documentoId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM politica_acceso p " +
            "WHERE p.documentoId = :documentoId " +
            "  AND p.estado = :estado " +
            "  AND p.fechaExpiracion > :ahora",
            Long.class
        );
        query.setParameter("documentoId", documentoId);
        query.setParameter("estado", EstadoPermiso.ACTIVO);
        query.setParameter("ahora", LocalDateTime.now());
        return query.getSingleResult();
    }

    /**
     * Elimina una política de acceso (generalmente no se usa, se prefiere revocar)
     */
    public void delete(UUID id) {
        politica_acceso politica = em.find(politica_acceso.class, id);
        if (politica != null) {
            em.remove(politica);
        }
    }
}
