package com.hcen.periferico.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO para mensajes JMS de confirmación de sincronización de usuarios.
 * Recibido desde componente central al periférico.
 *
 * Indica si la sincronización fue exitosa o falló, incluyendo mensaje de error si aplica.
 * Se serializa como JSON en TextMessage para mayor flexibilidad.
 *
 * @version 2.0 - Migrado a JSON
 */
public class UsuarioSaludConfirmacionMessage {

    private String cedula;
    private boolean exito;
    private String errorMensaje;
    private LocalDateTime timestamp;
    private String messageIdOriginal;

    /**
     * Constructor por defecto (requerido para deserialización JMS)
     */
    public UsuarioSaludConfirmacionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor completo
     */
    public UsuarioSaludConfirmacionMessage(String cedula, boolean exito,
                                          String errorMensaje, String messageIdOriginal) {
        this.cedula = cedula;
        this.exito = exito;
        this.errorMensaje = errorMensaje;
        this.messageIdOriginal = messageIdOriginal;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Factory method: Crea confirmación exitosa
     */
    public static UsuarioSaludConfirmacionMessage exitoso(String cedula, String messageIdOriginal) {
        return new UsuarioSaludConfirmacionMessage(cedula, true, null, messageIdOriginal);
    }

    /**
     * Factory method: Crea confirmación de error
     */
    public static UsuarioSaludConfirmacionMessage fallido(String cedula, String errorMensaje,
                                                         String messageIdOriginal) {
        return new UsuarioSaludConfirmacionMessage(cedula, false, errorMensaje, messageIdOriginal);
    }

    /**
     * Valida que el mensaje contenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return cedula != null && !cedula.trim().isEmpty();
    }

    // Getters y Setters

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }

    public void setErrorMensaje(String errorMensaje) {
        this.errorMensaje = errorMensaje;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageIdOriginal() {
        return messageIdOriginal;
    }

    public void setMessageIdOriginal(String messageIdOriginal) {
        this.messageIdOriginal = messageIdOriginal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioSaludConfirmacionMessage that = (UsuarioSaludConfirmacionMessage) o;
        return exito == that.exito &&
               Objects.equals(cedula, that.cedula) &&
               Objects.equals(messageIdOriginal, that.messageIdOriginal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cedula, exito, messageIdOriginal);
    }

    @Override
    public String toString() {
        return "UsuarioSaludConfirmacionMessage{" +
                "cedula='" + cedula + '\'' +
                ", exito=" + exito +
                ", errorMensaje='" + errorMensaje + '\'' +
                ", timestamp=" + timestamp +
                ", messageIdOriginal='" + messageIdOriginal + '\'' +
                '}';
    }
}
