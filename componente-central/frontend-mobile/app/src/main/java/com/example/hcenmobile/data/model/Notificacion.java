package com.example.hcenmobile.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Entidad Room que representa una notificación en la base de datos local
 */
@Entity(tableName = "notificaciones")
public class Notificacion {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String notificacionId; // ID del servidor
    private String tipo; // NUEVO_PEDIDO_ACCESO, NUEVO_ACCESO, ACCESO_HISTORIA
    private String titulo;
    private String mensaje;
    private String remitente; // Quien generó la notificación (profesional, clínica, etc.)
    private Date fechaHora;
    private boolean leida;
    private String datosAdicionales; // JSON con información extra

    public Notificacion() {
        this.fechaHora = new Date();
        this.leida = false;
    }

    // Constructor completo
    public Notificacion(String notificacionId, String tipo, String titulo, String mensaje,
                        String remitente, Date fechaHora, boolean leida, String datosAdicionales) {
        this.notificacionId = notificacionId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.remitente = remitente;
        this.fechaHora = fechaHora;
        this.leida = leida;
        this.datosAdicionales = datosAdicionales;
    }

    // Getters y Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNotificacionId() {
        return notificacionId;
    }

    public void setNotificacionId(String notificacionId) {
        this.notificacionId = notificacionId;
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

    public Date getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Date fechaHora) {
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
