package hcen.central.inus.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para mensajes JMS de confirmación de sincronización.
 *
 * Este mensaje es enviado desde el componente central al componente periférico
 * a través de la cola "SincronizacionConfirmaciones" para confirmar que un documento
 * fue procesado (exitosamente o con error).
 *
 * Flujo:
 * 1. Central procesa mensaje de DocumentoSincronizacionMessage
 * 2. Central registra documento en historia_clinica
 * 3. Central envía este mensaje de confirmación
 * 4. Periférico actualiza documento.hist_clinica_id y sincronizacion_pendiente
 *
 * @author Sistema HCEN
 * @version 1.0
 */
public class SincronizacionConfirmacionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID del documento clínico en el componente periférico
     */
    private UUID documentoId;

    /**
     * ID de la historia clínica en el componente central
     * (null si la sincronización falló)
     */
    private UUID historiaId;

    /**
     * ID de la clínica (tenant)
     */
    private UUID tenantId;

    /**
     * Cédula del paciente
     */
    private String cedula;

    /**
     * Indica si la sincronización fue exitosa
     */
    private boolean exito;

    /**
     * Mensaje de error (null si exito=true)
     */
    private String errorMensaje;

    /**
     * Timestamp de procesamiento en el central
     */
    private LocalDateTime timestamp;

    /**
     * ID del mensaje JMS original que se procesó
     */
    private String messageIdOriginal;

    /**
     * Constructor por defecto (requerido para serialización JMS)
     */
    public SincronizacionConfirmacionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor para confirmación exitosa
     *
     * @param documentoId ID del documento sincronizado
     * @param historiaId ID de la historia clínica central
     * @param tenantId ID de la clínica
     * @param cedula Cédula del paciente
     */
    public SincronizacionConfirmacionMessage(UUID documentoId, UUID historiaId, UUID tenantId, String cedula) {
        this.documentoId = documentoId;
        this.historiaId = historiaId;
        this.tenantId = tenantId;
        this.cedula = cedula;
        this.exito = true;
        this.errorMensaje = null;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor para confirmación con error
     *
     * @param documentoId ID del documento que falló
     * @param tenantId ID de la clínica
     * @param cedula Cédula del paciente
     * @param errorMensaje Descripción del error
     */
    public SincronizacionConfirmacionMessage(UUID documentoId, UUID tenantId, String cedula, String errorMensaje) {
        this.documentoId = documentoId;
        this.historiaId = null;
        this.tenantId = tenantId;
        this.cedula = cedula;
        this.exito = false;
        this.errorMensaje = errorMensaje;
        this.timestamp = LocalDateTime.now();
    }

    // ============================================================
    // FACTORY METHODS
    // ============================================================

    /**
     * Crea mensaje de confirmación exitosa
     */
    public static SincronizacionConfirmacionMessage exitoso(UUID documentoId, UUID historiaId, UUID tenantId, String cedula) {
        return new SincronizacionConfirmacionMessage(documentoId, historiaId, tenantId, cedula);
    }

    /**
     * Crea mensaje de confirmación con error
     */
    public static SincronizacionConfirmacionMessage fallido(UUID documentoId, UUID tenantId, String cedula, String errorMensaje) {
        return new SincronizacionConfirmacionMessage(documentoId, tenantId, cedula, errorMensaje);
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

    // ============================================================
    // MÉTODOS UTILITARIOS
    // ============================================================

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

    /**
     * Valida que el mensaje tenga todos los campos requeridos
     *
     * @return true si el mensaje es válido
     */
    public boolean isValid() {
        boolean basicFieldsValid = documentoId != null &&
                                    tenantId != null &&
                                    cedula != null &&
                                    !cedula.trim().isEmpty();

        if (!basicFieldsValid) {
            return false;
        }

        // Si es exitoso, debe tener historiaId
        if (exito && historiaId == null) {
            return false;
        }

        // Si falló, debe tener mensaje de error
        if (!exito && (errorMensaje == null || errorMensaje.trim().isEmpty())) {
            return false;
        }

        return true;
    }
}
