package hcen.central.inus.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para mensajes JMS de confirmación de sincronización enviados por el central.
 */
public class SincronizacionConfirmacionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID documentoId;
    private UUID historiaId;
    private UUID tenantId;
    private String cedula;
    private boolean exito;
    private String errorMensaje;
    private LocalDateTime timestamp;
    private String messageIdOriginal;

    public SincronizacionConfirmacionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public SincronizacionConfirmacionMessage(UUID documentoId, UUID historiaId, UUID tenantId, String cedula) {
        this.documentoId = documentoId;
        this.historiaId = historiaId;
        this.tenantId = tenantId;
        this.cedula = cedula;
        this.exito = true;
        this.timestamp = LocalDateTime.now();
    }

    public SincronizacionConfirmacionMessage(UUID documentoId, UUID tenantId, String cedula, String errorMensaje) {
        this.documentoId = documentoId;
        this.tenantId = tenantId;
        this.cedula = cedula;
        this.exito = false;
        this.errorMensaje = errorMensaje;
        this.timestamp = LocalDateTime.now();
    }

    public static SincronizacionConfirmacionMessage exitoso(UUID documentoId, UUID historiaId, UUID tenantId, String cedula) {
        return new SincronizacionConfirmacionMessage(documentoId, historiaId, tenantId, cedula);
    }

    public static SincronizacionConfirmacionMessage fallido(UUID documentoId, UUID tenantId, String cedula, String errorMensaje) {
        return new SincronizacionConfirmacionMessage(documentoId, tenantId, cedula, errorMensaje);
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public UUID getHistoriaId() {
        return historiaId;
    }

    public void setHistoriaId(UUID historiaId) {
        this.historiaId = historiaId;
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

    public boolean isValid() {
        return documentoId != null &&
               tenantId != null &&
               cedula != null &&
               !cedula.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "SincronizacionConfirmacionMessage{" +
                "documentoId=" + documentoId +
                ", historiaId=" + historiaId +
                ", tenantId=" + tenantId +
                ", cedula='" + cedula + '\'' +
                ", exito=" + exito +
                ", errorMensaje='" + errorMensaje + '\'' +
                ", timestamp=" + timestamp +
                ", messageIdOriginal='" + messageIdOriginal + '\'' +
                '}';
    }
}
