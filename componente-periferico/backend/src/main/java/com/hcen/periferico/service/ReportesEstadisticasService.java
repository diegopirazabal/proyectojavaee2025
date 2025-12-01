package com.hcen.periferico.service;

import com.hcen.periferico.dao.ClinicaDAO;
import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.ProfesionalSaludDAO;
import com.hcen.periferico.dao.SolicitudAccesoDocumentoDAO;
import com.hcen.periferico.dao.UsuarioSaludDAO;
import com.hcen.periferico.entity.clinica;
import com.hcen.periferico.entity.solicitud_acceso_documento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service que consolida métricas operativas de las clínicas registradas en el componente periférico.
 */
@Stateless
public class ReportesEstadisticasService {

    private static final Logger LOGGER = Logger.getLogger(ReportesEstadisticasService.class.getName());

    @EJB
    private ClinicaDAO clinicaDAO;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @EJB
    private DocumentoClinicoDAO documentoClinicoDAO;

    @EJB
    private ProfesionalSaludDAO profesionalSaludDAO;

    @EJB
    private SolicitudAccesoDocumentoDAO solicitudAccesoDocumentoDAO;

    /**
     * Calcula las métricas generales y por clínica usando consultas agregadas.
     * Optimizado para evitar N+1 queries: en vez de 1+4N queries, ejecuta solo 5 queries.
     */
    public Estadisticas generarEstadisticas() {
        Estadisticas estadisticas = new Estadisticas();
        estadisticas.setGeneratedAt(Instant.now().toString());

        List<ClinicaEstadistica> clinicaEstadisticas = new ArrayList<>();
        Totales totales = new Totales();

        try {
            // Query 1: Obtener todas las clínicas
            List<clinica> clinicas = clinicaDAO.findAll();

            if (clinicas.isEmpty()) {
                estadisticas.setTotals(totales);
                estadisticas.setClinicas(clinicaEstadisticas);
                estadisticas.setTotalClinicas(0);
                return estadisticas;
            }

            // Extraer lista de tenant IDs
            List<UUID> tenantIds = clinicas.stream()
                .map(clinica::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // Query 2: Contar pacientes por tenant (1 query con GROUP BY)
            Map<UUID, Long> pacientesPorTenant = usuarioSaludDAO.countByTenantIdBatch(tenantIds);

            // Query 3: Contar documentos por tenant (1 query con GROUP BY)
            Map<UUID, Long> documentosPorTenant = documentoClinicoDAO.countByTenantIdBatch(tenantIds);

            // Query 4: Contar profesionales por tenant (1 query con GROUP BY)
            Map<UUID, Long> profesionalesPorTenant = profesionalSaludDAO.countByTenantIdBatch(tenantIds);

            // Query 5: Contar accesos aprobados por tenant (1 query con GROUP BY)
            Map<UUID, Long> accesosPorTenant = solicitudAccesoDocumentoDAO.countByTenantIdAndEstadoBatch(
                tenantIds,
                solicitud_acceso_documento.EstadoSolicitud.APROBADA
            );

            // Iterar sobre clínicas y construir estadísticas (sin más queries a BD)
            for (clinica registro : clinicas) {
                UUID tenantId = registro.getTenantId();
                if (tenantId == null) {
                    continue;
                }

                ClinicaEstadistica detalle = new ClinicaEstadistica();
                detalle.setTenantId(tenantId.toString());
                detalle.setNombre(registro.getNombre());
                detalle.setEmail(registro.getEmail());

                // Obtener counts de los Maps (defaultea a 0 si no existe)
                long pacientes = pacientesPorTenant.getOrDefault(tenantId, 0L);
                long documentos = documentosPorTenant.getOrDefault(tenantId, 0L);
                long profesionales = profesionalesPorTenant.getOrDefault(tenantId, 0L);
                long accesos = accesosPorTenant.getOrDefault(tenantId, 0L);

                detalle.setPacientes(pacientes);
                detalle.setDocumentos(documentos);
                detalle.setProfesionales(profesionales);
                detalle.setAccesosDocumentos(accesos);

                // Sumar a totales
                totales.incrementarPacientes(pacientes);
                totales.incrementarDocumentos(documentos);
                totales.incrementarProfesionales(profesionales);
                totales.incrementarAccesos(accesos);

                clinicaEstadisticas.add(detalle);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar estadísticas de reportes", e);
        }

        estadisticas.setTotals(totales);
        estadisticas.setClinicas(clinicaEstadisticas);
        estadisticas.setTotalClinicas(clinicaEstadisticas.size());
        return estadisticas;
    }

    /**
     * Genera estadísticas con paginación de clínicas (lazy loading).
     * Los totales se calculan siempre para todas las clínicas, solo se paginan las clínicas.
     *
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Estadísticas con totales globales y clínicas de la página solicitada
     */
    public Estadisticas generarEstadisticasPaginadas(int page, int size) {
        Estadisticas estadisticas = new Estadisticas();
        estadisticas.setGeneratedAt(Instant.now().toString());

        try {
            // 1. Contar total de clínicas (para rowCount del lazy model)
            long totalClinicas = clinicaDAO.countAll();
            estadisticas.setTotalClinicas((int) totalClinicas);

            if (totalClinicas == 0) {
                estadisticas.setTotals(new Totales());
                estadisticas.setClinicas(Collections.emptyList());
                return estadisticas;
            }

            // 2. Calcular TOTALES GLOBALES (todas las clínicas, no solo de la página)
            Totales totales = calcularTotalesGlobales();
            estadisticas.setTotals(totales);

            // 3. Obtener clínicas de la página actual con PAGINACIÓN
            List<clinica> clinicas = clinicaDAO.findAllPaginated(page, size);

            if (clinicas.isEmpty()) {
                estadisticas.setClinicas(Collections.emptyList());
                return estadisticas;
            }

            // 4. Extraer tenant IDs de la página actual
            List<UUID> tenantIds = clinicas.stream()
                .map(clinica::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 5. Obtener estadísticas SOLO de las clínicas de la página actual
            Map<UUID, Long> pacientesPorTenant = usuarioSaludDAO.countByTenantIdBatch(tenantIds);
            Map<UUID, Long> documentosPorTenant = documentoClinicoDAO.countByTenantIdBatch(tenantIds);
            Map<UUID, Long> profesionalesPorTenant = profesionalSaludDAO.countByTenantIdBatch(tenantIds);
            Map<UUID, Long> accesosPorTenant = solicitudAccesoDocumentoDAO.countByTenantIdAndEstadoBatch(
                tenantIds,
                solicitud_acceso_documento.EstadoSolicitud.APROBADA
            );

            // 6. Construir lista de clínicas de la página
            List<ClinicaEstadistica> clinicaEstadisticas = new ArrayList<>();
            for (clinica registro : clinicas) {
                UUID tenantId = registro.getTenantId();
                if (tenantId == null) {
                    continue;
                }

                ClinicaEstadistica detalle = new ClinicaEstadistica();
                detalle.setTenantId(tenantId.toString());
                detalle.setNombre(registro.getNombre());
                detalle.setEmail(registro.getEmail());
                detalle.setPacientes(pacientesPorTenant.getOrDefault(tenantId, 0L));
                detalle.setDocumentos(documentosPorTenant.getOrDefault(tenantId, 0L));
                detalle.setProfesionales(profesionalesPorTenant.getOrDefault(tenantId, 0L));
                detalle.setAccesosDocumentos(accesosPorTenant.getOrDefault(tenantId, 0L));

                clinicaEstadisticas.add(detalle);
            }

            estadisticas.setClinicas(clinicaEstadisticas);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar estadísticas paginadas", e);
        }

        return estadisticas;
    }

    /**
     * Calcula los totales globales (todas las clínicas, sin paginación)
     * Usado por generarEstadisticasPaginadas() para mantener totales consistentes
     *
     * @return Totales con counts globales de pacientes, documentos, profesionales y accesos
     */
    private Totales calcularTotalesGlobales() {
        Totales totales = new Totales();

        try {
            // Contar totales globales usando métodos que ya existen en los DAOs
            long totalPacientes = usuarioSaludDAO.countActivos();
            long totalDocumentos = documentoClinicoDAO.countAll();
            long totalProfesionales = profesionalSaludDAO.countAll();
            long totalAccesos = solicitudAccesoDocumentoDAO.countByEstado(
                solicitud_acceso_documento.EstadoSolicitud.APROBADA
            );

            totales.incrementarPacientes(totalPacientes);
            totales.incrementarDocumentos(totalDocumentos);
            totales.incrementarProfesionales(totalProfesionales);
            totales.incrementarAccesos(totalAccesos);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al calcular totales globales", e);
        }

        return totales;
    }

    // ===== DTOs para serialización =====

    public static class Estadisticas {
        private String generatedAt;
        private Totales totals = new Totales();
        private List<ClinicaEstadistica> clinicas = new ArrayList<>();
        private int totalClinicas;

        public String getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(String generatedAt) {
            this.generatedAt = generatedAt;
        }

        public Totales getTotals() {
            return totals;
        }

        public void setTotals(Totales totals) {
            this.totals = totals;
        }

        public List<ClinicaEstadistica> getClinicas() {
            return clinicas;
        }

        public void setClinicas(List<ClinicaEstadistica> clinicas) {
            this.clinicas = clinicas;
        }

        public int getTotalClinicas() {
            return totalClinicas;
        }

        public void setTotalClinicas(int totalClinicas) {
            this.totalClinicas = totalClinicas;
        }
    }

    public static class Totales {
        private long pacientes;
        private long documentos;
        private long accesosDocumentos;
        private long profesionales;

        public long getPacientes() {
            return pacientes;
        }

        public long getDocumentos() {
            return documentos;
        }

        public long getAccesosDocumentos() {
            return accesosDocumentos;
        }

        public long getProfesionales() {
            return profesionales;
        }

        public void incrementarPacientes(long valor) {
            this.pacientes += valor;
        }

        public void incrementarDocumentos(long valor) {
            this.documentos += valor;
        }

        public void incrementarAccesos(long valor) {
            this.accesosDocumentos += valor;
        }

        public void incrementarProfesionales(long valor) {
            this.profesionales += valor;
        }
    }

    public static class ClinicaEstadistica {
        private String tenantId;
        private String nombre;
        private String email;
        private long pacientes;
        private long documentos;
        private long accesosDocumentos;
        private long profesionales;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public long getPacientes() {
            return pacientes;
        }

        public void setPacientes(long pacientes) {
            this.pacientes = pacientes;
        }

        public long getDocumentos() {
            return documentos;
        }

        public void setDocumentos(long documentos) {
            this.documentos = documentos;
        }

        public long getAccesosDocumentos() {
            return accesosDocumentos;
        }

        public void setAccesosDocumentos(long accesosDocumentos) {
            this.accesosDocumentos = accesosDocumentos;
        }

        public long getProfesionales() {
            return profesionales;
        }

        public void setProfesionales(long profesionales) {
            this.profesionales = profesionales;
        }
    }
}
