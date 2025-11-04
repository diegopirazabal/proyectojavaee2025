package com.hcen.periferico.profesional.dto;

import java.io.Serializable;

public class clinica_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String nombre;

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
}
