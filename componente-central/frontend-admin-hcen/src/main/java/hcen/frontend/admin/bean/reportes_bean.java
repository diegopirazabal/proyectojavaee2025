package hcen.frontend.admin.bean;

import hcen.frontend.admin.dto.reportes_estadisticas_dto;
import hcen.frontend.admin.service.api_service;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

@Named
@ViewScoped
public class reportes_bean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(reportes_bean.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @Inject
    private api_service apiService;

    private reportes_estadisticas_dto estadisticas;
    private LazyDataModel<reportes_estadisticas_dto.ClinicaEstadistica> clinicasLazyModel;

    @PostConstruct
    public void init() {
        cargarTotales(false);
        inicializarLazyModel();
    }

    public void refrescarDatos() {
        cargarTotales(true);
        // El lazy model se recargará automáticamente en el próximo load()
    }

    /**
     * Carga solo los totales globales (sin clínicas).
     * Los totales son constantes y se calculan una sola vez.
     */
    private void cargarTotales(boolean mostrarMensaje) {
        try {
            // 1. Cargar estadísticas del periférico (pacientes, docs, profesionales, clínicas)
            estadisticas = apiService.obtenerReportesEstadisticasPaginadas(0, 1);

            // 2. Sobrescribir SOLO accesosDocumentos con el dato del central
            long accesosAprobadosCentral = apiService.obtenerAccesosAprobadosCentral();
            if (estadisticas != null && estadisticas.getTotals() != null) {
                estadisticas.getTotals().setAccesosDocumentos(accesosAprobadosCentral);
            }

            if (mostrarMensaje) {
                addInfoMessage("Estadísticas actualizadas", "La información fue sincronizada correctamente.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar totales", e);
            addErrorMessage("No se pudo cargar el reporte", "Detalle: " + e.getMessage());
        }
    }

    /**
     * Inicializa el LazyDataModel para paginación del lado del servidor.
     * El método load() se invoca automáticamente por PrimeFaces en cada cambio de página.
     */
    private void inicializarLazyModel() {
        clinicasLazyModel = new LazyDataModel<reportes_estadisticas_dto.ClinicaEstadistica>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                // Retornar el total de clínicas desde las estadísticas
                return estadisticas != null ? estadisticas.getTotalClinicas() : 0;
            }

            @Override
            public List<reportes_estadisticas_dto.ClinicaEstadistica> load(
                    int first,
                    int pageSize,
                    Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                try {
                    // Calcular número de página (0-indexed)
                    int page = first / pageSize;

                    LOGGER.log(Level.INFO, "Lazy loading clínicas: page={0}, size={1}, first={2}",
                        new Object[]{page, pageSize, first});

                    // Llamar al endpoint paginado
                    reportes_estadisticas_dto resultado = apiService.obtenerReportesEstadisticasPaginadas(page, pageSize);

                    if (resultado != null) {
                        // Actualizar SOLO metadata (no sobrescribir totales para preservar dato del central)
                        if (estadisticas == null) {
                            estadisticas = new reportes_estadisticas_dto();
                        }
                        estadisticas.setTotalClinicas(resultado.getTotalClinicas());
                        estadisticas.setGeneratedAt(resultado.getGeneratedAt());

                        // Actualizar rowCount del lazy model
                        this.setRowCount(resultado.getTotalClinicas());

                        // Sobrescribir accesos aprobados por clínica con dato del central
                        if (resultado.getClinicas() != null && !resultado.getClinicas().isEmpty()) {
                            Map<String, Long> accesosPorClinica = apiService.obtenerAccesosPorClinicaCentral();
                            for (reportes_estadisticas_dto.ClinicaEstadistica clinica : resultado.getClinicas()) {
                                Long accesos = accesosPorClinica.get(clinica.getTenantId());
                                clinica.setAccesosDocumentos(accesos != null ? accesos : 0L);
                            }
                        }

                        // Actualizar totales globales con dato del central (importante hacerlo al final)
                        long accesosAprobadosCentral = apiService.obtenerAccesosAprobadosCentral();
                        if (resultado.getTotals() != null) {
                            estadisticas.setTotals(resultado.getTotals());
                            estadisticas.getTotals().setAccesosDocumentos(accesosAprobadosCentral);
                        }

                        // Retornar clínicas de esta página
                        return resultado.getClinicas() != null
                            ? resultado.getClinicas()
                            : Collections.emptyList();
                    }

                    return Collections.emptyList();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error en lazy loading de clínicas", e);
                    addErrorMessage("Error al cargar datos", "No se pudieron cargar las clínicas de esta página.");
                    return Collections.emptyList();
                }
            }
        };
    }

    public reportes_estadisticas_dto.Totales getTotales() {
        return estadisticas != null && estadisticas.getTotals() != null
            ? estadisticas.getTotals()
            : new reportes_estadisticas_dto.Totales();
    }

    public LazyDataModel<reportes_estadisticas_dto.ClinicaEstadistica> getClinicasLazyModel() {
        return clinicasLazyModel;
    }

    public String getGeneratedAtFormatted() {
        if (estadisticas == null || estadisticas.getGeneratedAt() == null) {
            return "-";
        }
        try {
            Instant instant = Instant.parse(estadisticas.getGeneratedAt());
            return TIMESTAMP_FORMATTER.format(instant);
        } catch (Exception e) {
            return estadisticas.getGeneratedAt();
        }
    }

    public reportes_estadisticas_dto getEstadisticas() {
        return estadisticas;
    }

    private void addInfoMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }
}
