package com.hcen.periferico.portal.dto;

public class ClinicaDTO {
    private String tenantId;
    private String nombre;

    public ClinicaDTO() {}
    public ClinicaDTO(String tenantId, String nombre) { this.tenantId = tenantId; this.nombre = nombre; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
