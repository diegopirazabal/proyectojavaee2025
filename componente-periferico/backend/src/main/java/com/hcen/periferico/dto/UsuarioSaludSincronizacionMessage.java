package com.hcen.periferico.dto;

import com.hcen.periferico.enums.TipoDocumento;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO para mensajes JMS de sincronización de usuarios de salud.
 * Enviado desde componente periférico al central.
 *
 * Contiene solo los datos mínimos necesarios:
 * - cédula del usuario
 * - tipo de documento
 */
public class UsuarioSaludSincronizacionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private TipoDocumento tipoDocumento;
    private LocalDateTime timestamp;

    /**
     * Constructor por defecto (requerido para deserialización JMS)
     */
    public UsuarioSaludSincronizacionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros
     */
    public UsuarioSaludSincronizacionMessage(String cedula, TipoDocumento tipoDocumento) {
        this.cedula = cedula;
        this.tipoDocumento = tipoDocumento;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Valida que el mensaje contenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return cedula != null && !cedula.trim().isEmpty() &&
               tipoDocumento != null;
    }

    // Getters y Setters

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioSaludSincronizacionMessage that = (UsuarioSaludSincronizacionMessage) o;
        return Objects.equals(cedula, that.cedula) &&
               tipoDocumento == that.tipoDocumento;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cedula, tipoDocumento);
    }

    @Override
    public String toString() {
        return "UsuarioSaludSincronizacionMessage{" +
                "cedula='" + cedula + '\'' +
                ", tipoDocumento=" + tipoDocumento +
                ", timestamp=" + timestamp +
                '}';
    }
}
