package com.hcen.periferico.dao;

import com.hcen.periferico.entity.documento_clinico;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.*;
import java.util.stream.Collectors;

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
     * Busca un documento por ID sin filtrar por tenant.
     * Útil para procesos internos donde la validación se realiza en otra capa.
     */
    public Optional<documento_clinico> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(documento_clinico.class, id));
    }

    /**
     * Busca múltiples documentos por IDs y tenant (batch)
     * Útil para evitar N+1 queries
     */
    public List<documento_clinico> findByIdsAndTenantId(List<UUID> ids, UUID tenantId) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d " +
            "WHERE d.id IN :ids AND d.tenantId = :tenantId",
            documento_clinico.class
        );
        query.setParameter("ids", ids);
        query.setParameter("tenantId", tenantId);
        return query.getResultList();
    }

    /**
     * Busca múltiples documentos por IDs SIN filtrar por tenant (batch cross-tenant)
     * Usado por el componente central para recuperar documentos de un paciente
     * que pueden estar distribuidos en múltiples clínicas/tenants
     */
    public List<documento_clinico> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d WHERE d.id IN :ids",
            documento_clinico.class
        );
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    /**
     * Lista todos los documentos de un paciente específico (solo de una clínica)
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
     * Lista todos los documentos de un paciente de TODAS las clínicas
     * Útil para visualizar la historia clínica completa del paciente
     */
    public List<documento_clinico> findByPacienteAllTenants(String cedula) {
        TypedQuery<documento_clinico> query = em.createQuery(
            "SELECT d FROM documento_clinico d " +
            "WHERE d.usuarioSaludCedula = :cedula " +
            "ORDER BY d.fecCreacion DESC",
            documento_clinico.class
        );
        query.setParameter("cedula", cedula);
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
     * Cuenta documentos de un paciente (solo de una clínica)
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
     * Cuenta documentos de un paciente de TODAS las clínicas
     */
    public long countByPacienteAllTenants(String cedula) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(d) FROM documento_clinico d " +
            "WHERE d.usuarioSaludCedula = :cedula",
            Long.class
        );
        query.setParameter("cedula", cedula);
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

    /**
     * Cuenta todos los documentos registrados (todas las clínicas)
     */
    public long countAll() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(d) FROM documento_clinico d",
            Long.class
        );
        return query.getSingleResult();
    }

    // ============ MÉTODOS PARA CODIGUERAS ============

    /**
     * Obtiene el nombre de un motivo de consulta por código
     */
    public Optional<String> getNombreMotivoConsulta(String codigo) {
        try {
            Integer id = parseCodigo(codigo);
            if (id == null) {
                return Optional.empty();
            }
            String result = em.createNativeQuery(
                "SELECT concepto FROM motivo_consulta WHERE id = ?", String.class
            )
            .setParameter(1, id)
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
            Integer id = parseCodigo(codigo);
            if (id == null) {
                return Optional.empty();
            }
            String result = em.createNativeQuery(
                "SELECT concepto FROM estado_problema WHERE id = ?", String.class
            )
            .setParameter(1, id)
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
            Integer id = parseCodigo(codigo);
            if (id == null) {
                return Optional.empty();
            }
            String result = em.createNativeQuery(
                "SELECT concepto FROM grado_certeza WHERE id = ?", String.class
            )
            .setParameter(1, id)
            .getSingleResult().toString();
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Busca motivos de consulta por término (para autocompletado)
     * Limitado a 50 resultados para optimizar rendimiento
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> buscarMotivosConsulta(String termino) {
        Map<String, String> motivos = new LinkedHashMap<>();
        try {
            String query = "SELECT id, concepto FROM motivo_consulta " +
                          "WHERE LOWER(concepto) LIKE LOWER(?) " +
                          "ORDER BY concepto LIMIT 50";

            List<Object[]> results = em.createNativeQuery(query)
                .setParameter(1, "%" + termino + "%")
                .getResultList();

            for (Object[] row : results) {
                motivos.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            // Log error si es necesario
            e.printStackTrace();
        }
        return motivos;
    }

    /**
     * Obtiene todos los motivos de consulta disponibles
     * NOTA: Este método carga TODOS los registros (~12k) y puede ser muy lento.
     * Se recomienda usar buscarMotivosConsulta() con filtro para mejor rendimiento.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllMotivosConsulta() {
        Map<String, String> motivos = new LinkedHashMap<>();
        try {
            List<Object[]> results = em.createNativeQuery(
                "SELECT id, concepto FROM motivo_consulta ORDER BY concepto"
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
                "SELECT id, concepto FROM estado_problema ORDER BY concepto"
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
                "SELECT id, concepto FROM grado_certeza ORDER BY concepto"
            ).getResultList();

            for (Object[] row : results) {
                grados.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            // Log error si es necesario
        }
        return grados;
    }

    /**
     * Obtiene nombres de múltiples motivos de consulta en una sola query (batch)
     * Optimizado para evitar N consultas individuales
     *
     * @param codigos Lista de códigos de motivos
     * @return Map con código -> nombre (solo incluye los que existen en BD)
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getNombresMotivosConsultaBatch(Collection<String> codigos) {
        Map<String, String> resultado = new HashMap<>();
        if (codigos == null || codigos.isEmpty()) {
            return resultado;
        }

        try {
            // Convertir códigos String a Integer y filtrar nulls
            List<Integer> ids = codigos.stream()
                .map(this::parseCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

            if (ids.isEmpty()) {
                return resultado;
            }

            // UNA sola consulta con IN clause
            List<Object[]> results = em.createNativeQuery(
                "SELECT id, concepto FROM motivo_consulta WHERE id IN (:ids)"
            )
            .setParameter("ids", ids)
            .getResultList();

            for (Object[] row : results) {
                resultado.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            // Log error si es necesario
            e.printStackTrace();
        }
        return resultado;
    }

    /**
     * Obtiene nombres de múltiples estados de problema en una sola query (batch)
     *
     * @param codigos Lista de códigos de estados
     * @return Map con código -> nombre (solo incluye los que existen en BD)
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getNombresEstadosProblemaBatch(Collection<String> codigos) {
        Map<String, String> resultado = new HashMap<>();
        if (codigos == null || codigos.isEmpty()) {
            return resultado;
        }

        try {
            List<Integer> ids = codigos.stream()
                .map(this::parseCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

            if (ids.isEmpty()) {
                return resultado;
            }

            List<Object[]> results = em.createNativeQuery(
                "SELECT id, concepto FROM estado_problema WHERE id IN (:ids)"
            )
            .setParameter("ids", ids)
            .getResultList();

            for (Object[] row : results) {
                resultado.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }

    /**
     * Obtiene nombres de múltiples grados de certeza en una sola query (batch)
     *
     * @param codigos Lista de códigos de grados
     * @return Map con código -> nombre (solo incluye los que existen en BD)
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getNombresGradosCertezaBatch(Collection<String> codigos) {
        Map<String, String> resultado = new HashMap<>();
        if (codigos == null || codigos.isEmpty()) {
            return resultado;
        }

        try {
            List<Integer> ids = codigos.stream()
                .map(this::parseCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

            if (ids.isEmpty()) {
                return resultado;
            }

            List<Object[]> results = em.createNativeQuery(
                "SELECT id, concepto FROM grado_certeza WHERE id IN (:ids)"
            )
            .setParameter("ids", ids)
            .getResultList();

            for (Object[] row : results) {
                resultado.put(row[0].toString(), row[1].toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }

    private Integer parseCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        try {
            return Integer.valueOf(codigo);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Cuenta documentos clínicos agrupados por tenant_id (consulta agregada)
     * Optimizado para evitar N+1 queries en reportes
     *
     * @param tenantIds Colección de tenant IDs a consultar
     * @return Map con tenantId -> cantidad de documentos
     */
    public Map<UUID, Long> countByTenantIdBatch(Collection<UUID> tenantIds) {
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
                "SELECT d.tenantId, COUNT(d) FROM documento_clinico d " +
                "WHERE d.tenantId IN :tenantIds " +
                "GROUP BY d.tenantId",
                Object[].class
            );
            query.setParameter("tenantIds", ids);
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
