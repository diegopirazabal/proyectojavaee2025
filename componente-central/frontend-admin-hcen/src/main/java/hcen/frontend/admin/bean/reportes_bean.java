package hcen.frontend.admin.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.plugins.Plugins;
import org.primefaces.model.charts.optionconfig.tooltip.Tooltip;

@Named
@ViewScoped
public class reportes_bean implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ReporteMetric> resumenMetricas;
    private List<ClinicaActividad> actividadClinicas;
    private BarChartModel usuariosClinicasChart;

    @PostConstruct
    public void init() {
        resumenMetricas = List.of(
            new ReporteMetric("Clínicas integradas", "23", "+2 esta semana", true),
            new ReporteMetric("Usuarios activos", "1.540", "+8% vs último mes", true),
            new ReporteMetric("Alertas pendientes", "4", "Severidad media", false)
        );

        actividadClinicas = List.of(
            new ClinicaActividad("Salud Total", 182, 97, "Operativa"),
            new ClinicaActividad("Red Médica Norte", 153, 84, "Operativa"),
            new ClinicaActividad("Clínica Sur", 131, 65, "Operativa"),
            new ClinicaActividad("Hospital Universitario", 90, 51, "Monitoreo")
        );

        construirGraficoUsuarios();
    }

    private void construirGraficoUsuarios() {
        usuariosClinicasChart = new BarChartModel();
        ChartData data = new ChartData();

        BarChartDataSet dataSet = new BarChartDataSet();
        dataSet.setLabel("Usuarios activos por trimestre");
        dataSet.setBackgroundColor(Arrays.asList(
            "rgba(75,108,183,0.8)",
            "rgba(86,134,197,0.8)",
            "rgba(97,148,208,0.8)",
            "rgba(111,168,219,0.8)"
        ));
        dataSet.setData(Arrays.asList(420, 460, 515, 548));

        data.addChartDataSet(dataSet);
        data.setLabels(Arrays.asList("Q1", "Q2", "Q3", "Q4"));
        usuariosClinicasChart.setData(data);

        BarChartOptions options = new BarChartOptions();
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);
        options.setAspectRatio(1.5);

        Tooltip tooltip = new Tooltip();
        tooltip.setEnabled(true);

        Legend legend = new Legend();
        legend.setDisplay(false);

        Plugins plugins = new Plugins();
        plugins.setTooltip(tooltip);
        plugins.setLegend(legend);
        options.setPlugins(plugins);

        CartesianScales scales = new CartesianScales();
        CartesianLinearAxes yAxes = new CartesianLinearAxes();
        CartesianLinearTicks ticks = new CartesianLinearTicks();
        ticks.setBeginAtZero(true);
        yAxes.setTicks(ticks);
        scales.addYAxesData(yAxes);
        options.setScales(scales);

        usuariosClinicasChart.setOptions(options);
    }

    public List<ReporteMetric> getResumenMetricas() {
        return resumenMetricas;
    }

    public List<ClinicaActividad> getActividadClinicas() {
        return actividadClinicas;
    }

    public BarChartModel getUsuariosClinicasChart() {
        return usuariosClinicasChart;
    }

    public static class ReporteMetric implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final String value;
        private final String trendText;
        private final boolean positive;

        public ReporteMetric(String label, String value, String trendText, boolean positive) {
            this.label = label;
            this.value = value;
            this.trendText = trendText;
            this.positive = positive;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public String getTrendText() {
            return trendText;
        }

        public boolean isPositive() {
            return positive;
        }
    }

    public static class ClinicaActividad implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String nombre;
        private final int documentos;
        private final int usuariosActivos;
        private final String estado;

        public ClinicaActividad(String nombre, int documentos, int usuariosActivos, String estado) {
            this.nombre = nombre;
            this.documentos = documentos;
            this.usuariosActivos = usuariosActivos;
            this.estado = estado;
        }

        public String getNombre() {
            return nombre;
        }

        public int getDocumentos() {
            return documentos;
        }

        public int getUsuariosActivos() {
            return usuariosActivos;
        }

        public String getEstado() {
            return estado;
        }
    }
}
