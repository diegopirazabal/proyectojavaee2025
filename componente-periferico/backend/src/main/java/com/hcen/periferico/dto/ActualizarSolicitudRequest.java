package com.hcen.periferico.dto;

import java.io.Serializable;

/**
 * DTO para request de actualización de estado de solicitudes de acceso
 * Usado por el componente central para notificar cambios de estado (aprobación/rechazo)
 */
public class ActualizarSolicitudRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * UUID de la clínica (tenant_id)
     * Usado para validación de seguridad multi-tenant
     */
    private String tenantId;

    // Constructores

    public ActualizarSolicitudRequest() {
    }

    public ActualizarSolicitudRequest(String tenantId) {
        this.tenantId = tenantId;
    }

    // Getters y Setters

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "ActualizarSolicitudRequest{" +
                "tenantId='" + tenantId + '\'' +
                '}';
    }
}
