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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private List<reportes_estadisticas_dto.ClinicaEstadistica> clinicas = new ArrayList<>();

    @PostConstruct
    public void init() {
        cargarEstadisticas(false);
    }

    public void refrescarDatos() {
        cargarEstadisticas(true);
    }

    private void cargarEstadisticas(boolean mostrarMensaje) {
        try {
            estadisticas = apiService.obtenerReportesEstadisticas();
            clinicas = estadisticas != null && estadisticas.getClinicas() != null
                ? estadisticas.getClinicas()
                : Collections.emptyList();
            if (mostrarMensaje) {
                addInfoMessage("Estadísticas actualizadas", "La información fue sincronizada correctamente.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar estadísticas", e);
            clinicas = Collections.emptyList();
            addErrorMessage("No se pudo cargar el reporte", "Detalle: " + e.getMessage());
        }
    }

    public reportes_estadisticas_dto.Totales getTotales() {
        return estadisticas != null && estadisticas.getTotals() != null
            ? estadisticas.getTotals()
            : new reportes_estadisticas_dto.Totales();
    }

    public List<reportes_estadisticas_dto.ClinicaEstadistica> getClinicas() {
        return clinicas;
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
