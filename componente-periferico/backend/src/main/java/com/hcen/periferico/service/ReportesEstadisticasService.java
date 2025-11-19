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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Calcula las métricas generales y por clínica.
     */
    public Estadisticas generarEstadisticas() {
        Estadisticas estadisticas = new Estadisticas();
        estadisticas.setGeneratedAt(Instant.now().toString());

        List<ClinicaEstadistica> clinicaEstadisticas = new ArrayList<>();
        Totales totales = new Totales();

        try {
            List<clinica> clinicas = clinicaDAO.findAll();
            for (clinica registro : clinicas) {
                UUID tenantId = registro.getTenantId();
                if (tenantId == null) {
                    continue;
                }

                ClinicaEstadistica detalle = new ClinicaEstadistica();
                detalle.setTenantId(tenantId.toString());
                detalle.setNombre(registro.getNombre());
                detalle.setEmail(registro.getEmail());

                long pacientes = safeCount(() -> usuarioSaludDAO.countByTenantId(tenantId));
                long documentos = safeCount(() -> documentoClinicoDAO.countByTenantId(tenantId));
                long profesionales = safeCount(() -> profesionalSaludDAO.countByTenantId(tenantId));
                long accesos = safeCount(() -> solicitudAccesoDocumentoDAO.countByTenantIdAndEstado(
                    tenantId,
                    solicitud_acceso_documento.EstadoSolicitud.APROBADA
                ));

                detalle.setPacientes(pacientes);
                detalle.setDocumentos(documentos);
                detalle.setProfesionales(profesionales);
                detalle.setAccesosDocumentos(accesos);

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

    private long safeCount(CountSupplier supplier) {
        try {
            return supplier.getAsLong();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo calcular una de las métricas del reporte", e);
            return 0L;
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        long getAsLong();
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
