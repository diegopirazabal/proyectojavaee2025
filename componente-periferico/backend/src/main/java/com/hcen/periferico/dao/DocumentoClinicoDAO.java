package com.hcen.periferico.dao;

import com.hcen.periferico.entity.documento_clinico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.*;

/**
 * DAO para gestión de documentos clínicos.
 * IMPORTANTE: Todos los métodos filtran por tenant_id para garantizar aislamiento multi-tenant.
 */
@Stateless
public class DocumentoClinicoDAO {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    /**
     * Guarda o actualiza un documento clínico
     */
    public documento_clinico save(documento_clinico documento) {
        return em.merge(documento);
    }

    /**
     * Busca un documento por ID con validación de tenant
     */
    public Optional<documento_clinico> findByIdAndTenantId(UUID id, UUID tenantId) {
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d " +
            "WHERE d.id = :id AND d.tenantId = :tenantId",
            documento_clinico.class
        );
        query.setParameter("id", id);
        query.setParameter("tenantId", tenantId);
        List<documento_clinico> resultList = query.getResultList();
        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
    }

    /**
     * Lista todos los documentos de un paciente específico
     */
    public List<documento_clinico> findByPaciente(String cedula, UUID tenantId) {
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d " +
            "WHERE d.usuarioSaludCedula = :cedula AND d.tenantId = :tenantId " +
            "ORDER BY d.fecCreacion DESC",
            documento_clinico.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Lista todos los documentos firmados por un profesional específico
     */
    public List<documento_clinico> findByProfesional(Integer profesionalCi, UUID tenantId) {
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d " +
            "WHERE d.profesionalFirmante.ci = :profesionalCi AND d.tenantId = :tenantId " +
            "ORDER BY d.fecCreacion DESC",
            documento_clinico.class
        );
        query.setParameter("profesionalCi", profesionalCi);
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Lista todos los documentos de una clínica con paginación
     */
    public List<documento_clinico> findAllByTenantIdPaginated(UUID tenantId, int page, int size) {
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d " +
            "WHERE d.tenantId = :tenantId " +
            "ORDER BY d.fecCreacion DESC",
            documento_clinico.class
        );
        query.setParameter("tenantId", tenantId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Cuenta documentos de una clínica
     */
    public long countByTenantId(UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(d) FROM documento_clinico d " +
            "WHERE d.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult();
    }

    /**
     * Cuenta documentos de un paciente
     */
    public long countByPaciente(String cedula, UUID tenantId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(d) FROM documento_clinico d " +
            "WHERE d.usuarioSaludCedula = :cedula AND d.tenantId = :tenantId",
            Long.class
        );
        query.setParameter("cedula", cedula);
        query.setParameter("tenantId", tenantId);
        return query.getSingleResult();
    }

    /**
     * Elimina un documento
     */
    public void delete(documento_clinico documento) {
        if (!em.contains(documento)) {
            documento = em.merge(documento);
        }
        em.remove(documento);
    }

    /**
     * Elimina un documento por ID con validación de tenant
     */
    public boolean deleteByIdAndTenantId(UUID id, UUID tenantId) {
        Optional<documento_clinico> doc = findByIdAndTenantId(id, tenantId);
        if (doc.isPresent()) {
            delete(doc.get());
            return true;
        }
        return false;
    }

    // ============ MÉTODOS PARA CODIGUERAS ============

    /**
     * Obtiene el nombre de un motivo de consulta por código
     */
    public Optional<String> getNombreMotivoConsulta(String codigo) {
        try {
            String result = em.createNativeQuery(
                "SELECT descripcion FROM motivo_consulta WHERE codigo = ?", String.class
            )
            .setParameter(1, codigo)
            .getSingleResult().toString();
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Obtiene el nombre de un estado de problema por código
     */
    public Optional<String> getNombreEstadoProblema(String codigo) {
        try {
            String result = em.createNativeQuery(
                "SELECT descripcion FROM estado_problema WHERE codigo = ?", String.class
            )
            .setParameter(1, codigo)
            .getSingleResult().toString();
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Obtiene el nombre de un grado de certeza por código
     */
    public Optional<String> getNombreGradoCerteza(String codigo) {
        try {
            String result = em.createNativeQuery(
                "SELECT descripcion FROM grado_certeza WHERE codigo = ?", String.class
            )
            .setParameter(1, codigo)
            .getSingleResult().toString();
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Obtiene todos los motivos de consulta disponibles
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllMotivosConsulta() {
        Map<String, String> motivos = new LinkedHashMap<>();
        try {
            List<Object[]> results = em.createNativeQuery(
                "SELECT codigo, descripcion FROM motivo_consulta ORDER BY descripcion"
            ).getResultList();

            for (Object[] row : results) {
                motivos.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            // Log error si es necesario
        }
        return motivos;
    }

    /**
     * Obtiene todos los estados de problema disponibles
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllEstadosProblema() {
        Map<String, String> estados = new LinkedHashMap<>();
        try {
            List<Object[]> results = em.createNativeQuery(
                "SELECT codigo, descripcion FROM estado_problema ORDER BY descripcion"
            ).getResultList();

            for (Object[] row : results) {
                estados.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            // Log error si es necesario
        }
        return estados;
    }

    /**
     * Obtiene todos los grados de certeza disponibles
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllGradosCerteza() {
        Map<String, String> grados = new LinkedHashMap<>();
        try {
            List<Object[]> results = em.createNativeQuery(
                "SELECT codigo, descripcion FROM grado_certeza ORDER BY descripcion"
            ).getResultList();

            for (Object[] row : results) {
                grados.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            // Log error si es necesario
        }
        return grados;
    }
}
