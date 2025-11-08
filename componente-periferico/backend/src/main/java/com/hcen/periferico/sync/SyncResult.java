package com.hcen.periferico.sync;

import java.io.Serializable;

/**
 * Resultado de una operación de sincronización con el componente central.
 * Usado por ICentralSyncAdapter para reportar el estado de la sincronización.
 */
public class SyncResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean exito;
    private boolean yaExistia;
    private String mensaje;
    private String errorDetalle;

    // Constructores
    public SyncResult() {
    }

    public SyncResult(boolean exito, boolean yaExistia, String mensaje) {
        this.exito = exito;
        this.yaExistia = yaExistia;
        this.mensaje = mensaje;
    }

    // Factory methods para casos comunes
    public static SyncResult exitoso(String mensaje) {
        return new SyncResult(true, false, mensaje);
    }

    public static SyncResult exitosoYaExistia(String mensaje) {
        return new SyncResult(true, true, mensaje);
    }

    public static SyncResult fallido(String mensaje, String errorDetalle) {
        SyncResult result = new SyncResult(false, false, mensaje);
        result.setErrorDetalle(errorDetalle);
        return result;
    }

    public static SyncResult fallido(String mensaje) {
        return new SyncResult(false, false, mensaje);
    }

    /**
     * Representa una operación que quedó encolada para procesamiento asíncrono.
     * Se considera exitosa para la capa de sincronización, pero la confirmación
     * final dependerá del consumidor de la cola.
     */
    public static SyncResult encolado(String mensaje) {
        return new SyncResult(true, false, mensaje);
    }

    // Getters y Setters
    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public boolean isYaExistia() {
        return yaExistia;
    }

    public void setYaExistia(boolean yaExistia) {
        this.yaExistia = yaExistia;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getErrorDetalle() {
        return errorDetalle;
    }

    public void setErrorDetalle(String errorDetalle) {
        this.errorDetalle = errorDetalle;
    }

    @Override
    public String toString() {
        return "SyncResult{" +
                "exito=" + exito +
                ", yaExistia=" + yaExistia +
                ", mensaje='" + mensaje + '\'' +
                (errorDetalle != null ? ", errorDetalle='" + errorDetalle + '\'' : "") +
                '}';
    }
}
