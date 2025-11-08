package hcen.central.inus.dto;

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
 */
public class DocumentoSincronizacionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID documentoId;
    private UUID tenantId;
    private String cedula;
    private LocalDateTime timestamp;
    private String messageId;

    public DocumentoSincronizacionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public DocumentoSincronizacionMessage(UUID documentoId, UUID tenantId, String cedula) {
        this.documentoId = documentoId;
        this.tenantId = tenantId;
        this.cedula = cedula;
        this.timestamp = LocalDateTime.now();
    }

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

    @Override
    public String toString() {
        return "DocumentoSincronizacionMessage{" +
                "documentoId=" + documentoId +
                ", tenantId=" + tenantId +
                ", cedula='" + cedula + '\'' +
                ", timestamp=" + timestamp +
                ", messageId='" + messageId + '\'' +
                '}';
    }

    public boolean isValid() {
        return documentoId != null &&
               tenantId != null &&
               cedula != null &&
               !cedula.trim().isEmpty();
    }
}
