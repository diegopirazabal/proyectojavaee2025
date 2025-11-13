package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request para rechazar una solicitud de acceso a documento clínico
 */
public class RechazarSolicitudRequest {

    @SerializedName("notificacionId")
    private String notificacionId;

    @SerializedName("motivo")
    private String motivo; // Opcional

    // Constructor vacío
    public RechazarSolicitudRequest() {
    }

    // Constructor con ID
    public RechazarSolicitudRequest(String notificacionId) {
        this.notificacionId = notificacionId;
    }

    // Constructor completo
    public RechazarSolicitudRequest(String notificacionId, String motivo) {
        this.notificacionId = notificacionId;
        this.motivo = motivo;
    }

    // Getters y Setters
    public String getNotificacionId() {
        return notificacionId;
    }

    public void setNotificacionId(String notificacionId) {
        this.notificacionId = notificacionId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
