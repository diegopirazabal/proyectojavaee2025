package hcen.frontend.admin.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class reportes_estadisticas_dto implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public static class Totales implements Serializable {
        private long pacientes;
        private long documentos;
        private long accesosDocumentos;
        private long profesionales;

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

    public static class ClinicaEstadistica implements Serializable {
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
