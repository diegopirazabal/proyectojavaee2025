package com.hcen.periferico.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para mensajes JMS de sincronización de documentos clínicos.
 *
 * Este mensaje se envía desde el componente periférico al componente central
 * a través de la cola "DocumentosSincronizacion" para solicitar la centralización
 * de un documento clínico en la historia clínica nacional.
 *
 * Flujo:
 * 1. Periférico crea documento local
 * 2. Periférico envía este mensaje a la cola
 * 3. Central consume mensaje y registra documento en historia_clinica
 * 4. Central envía confirmación (SincronizacionConfirmacionMessage)
 *
 * @author Sistema HCEN
 * @version 1.0
 */
public class documento_sincronizacion_message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID del documento clínico en el componente periférico
     */
    private UUID documentoId;

    /**
     * ID de la clínica (tenant) que creó el documento
     */
    private UUID tenantId;

    /**
     * Cédula de identidad del paciente dueño del documento
     */
    private String cedula;

    /**
     * Timestamp de creación del mensaje (para auditoría)
     */
    private LocalDateTime timestamp;

    /**
     * ID del mensaje JMS (asignado por ActiveMQ, se setea después del envío)
     */
    private String messageId;

    /**
     * Constructor por defecto (requerido para serialización JMS)
     */
    public documento_sincronizacion_message() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales
     *
     * @param documentoId ID del documento a sincronizar
     * @param tenantId ID de la clínica
     * @param cedula Cédula del paciente
     */
    public documento_sincronizacion_message(UUID documentoId, UUID tenantId, String cedula) {
        this.documentoId = documentoId;
        this.tenantId = tenantId;
        this.cedula = cedula;
        this.timestamp = LocalDateTime.now();
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    // ============================================================
    // MÉTODOS UTILITARIOS
    // ============================================================

    @Override
    public String toString() {
        return "documento_sincronizacion_message{" +
                "documentoId=" + documentoId +
                ", tenantId=" + tenantId +
                ", cedula='" + cedula + '\'' +
                ", timestamp=" + timestamp +
                ", messageId='" + messageId + '\'' +
                '}';
    }

    /**
     * Valida que el mensaje tenga todos los campos requeridos
     *
     * @return true si el mensaje es válido
     */
    public boolean isValid() {
        return documentoId != null &&
               tenantId != null &&
               cedula != null &&
               !cedula.trim().isEmpty();
    }
}
