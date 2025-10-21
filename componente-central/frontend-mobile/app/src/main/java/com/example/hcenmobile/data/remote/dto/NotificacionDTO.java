package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para recibir notificaciones desde el backend REST
 */
public class NotificacionDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("tipo")
    private String tipo;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("remitente")
    private String remitente;

    @SerializedName("fechaHora")
    private String fechaHora; // ISO 8601 format

    @SerializedName("leida")
    private boolean leida;

    @SerializedName("datosAdicionales")
    private String datosAdicionales;

    // Constructor vacío
    public NotificacionDTO() {
    }

    // Constructor completo
    public NotificacionDTO(String id, String tipo, String titulo, String mensaje,
                           String remitente, String fechaHora, boolean leida, String datosAdicionales) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.remitente = remitente;
        this.fechaHora = fechaHora;
        this.leida = leida;
        this.datosAdicionales = datosAdicionales;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public String getDatosAdicionales() {
        return datosAdicionales;
    }

    public void setDatosAdicionales(String datosAdicionales) {
        this.datosAdicionales = datosAdicionales;
    }
}
