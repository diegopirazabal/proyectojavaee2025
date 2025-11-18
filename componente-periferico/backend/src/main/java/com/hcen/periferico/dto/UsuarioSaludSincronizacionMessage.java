package com.hcen.periferico.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO para mensajes JMS de sincronización de usuarios de salud.
 *
 * Este mensaje se envía desde el componente periférico al componente central
 * a través de la cola "UsuarioSaludRegistrado" para registrar un nuevo usuario.
 *
 * NOTA: A diferencia de la implementación anterior, este DTO NO necesita estar
 * en el mismo package en ambos componentes. Se serializa como JSON en TextMessage,
 * lo que permite mayor flexibilidad y desacoplamiento.
 *
 * Flujo:
 * 1. Periférico registra usuario local
 * 2. Periférico envía este mensaje como JSON a la cola
 * 3. Central parsea el JSON y registra usuario en BD nacional
 * 4. Central envía confirmación JSON (UsuarioSaludConfirmacionMessage)
 *
 * @author Sistema HCEN
 * @version 2.0 - Migrado a JSON
 */
public class UsuarioSaludSincronizacionMessage {

    /**
     * Cédula de identidad del usuario (identificador único nacional)
     */
    private String cedula;

    /**
     * Tipo de documento como String (DO, PA, etc.)
     */
    private String tipoDocumento;

    /**
     * Timestamp de creación del mensaje (para auditoría)
     */
    private LocalDateTime timestamp;

    /**
     * Constructor por defecto (requerido para deserialización JSON)
     */
    public UsuarioSaludSincronizacionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros
     *
     * @param cedula Cédula del usuario
     * @param tipoDocumento Tipo de documento (DO, PA, etc.)
     */
    public UsuarioSaludSincronizacionMessage(String cedula, String tipoDocumento) {
        this.cedula = cedula;
        this.tipoDocumento = tipoDocumento;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Valida que el mensaje contenga los datos mínimos requeridos
     *
     * @return true si el mensaje es válido
     */
    public boolean isValid() {
        return cedula != null && !cedula.trim().isEmpty() &&
               tipoDocumento != null && !tipoDocumento.trim().isEmpty();
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // ============================================================
    // MÉTODOS UTILITARIOS
    // ============================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioSaludSincronizacionMessage that = (UsuarioSaludSincronizacionMessage) o;
        return Objects.equals(cedula, that.cedula) &&
               Objects.equals(tipoDocumento, that.tipoDocumento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cedula, tipoDocumento);
    }

    @Override
    public String toString() {
        return "UsuarioSaludSincronizacionMessage{" +
                "cedula='" + cedula + '\'' +
                ", tipoDocumento='" + tipoDocumento + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
