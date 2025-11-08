package com.hcen.periferico.messaging;

import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.dto.sincronizacion_confirmacion_message;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.documento_clinico;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean que consume confirmaciones de sincronización desde el componente central.
 *
 * Este MDB escucha la cola "SincronizacionConfirmaciones" y procesa las confirmaciones
 * enviadas desde el componente central después de centralizar un documento clínico.
 *
 * Flujo:
 * 1. Recibe confirmación (SincronizacionConfirmacionMessage)
 * 2. Si éxito: Actualiza documento.hist_clinica_id con ID retornado por el central
 * 3. Actualiza sincronizacion_pendiente: estado = RESUELTA (si éxito) o ERROR (si falló)
 * 4. Si excepción: ActiveMQ reintenta automáticamente
 *
 * Características:
 * - Transaccional: CMT (Container Managed Transaction)
 * - Idempotente: Puede procesar misma confirmación múltiples veces sin problemas
 * - Tolerante a fallos: Si no encuentra documento/auditoría, loguea pero no falla
 *
 * @author Sistema HCEN
 * @version 1.0
 */
@MessageDriven(
    name = "SincronizacionConfirmacionConsumer",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationLookup",
            propertyValue = "java:/jms/queue/SincronizacionConfirmaciones"
        ),
        @ActivationConfigProperty(
            propertyName = "destinationType",
            propertyValue = "jakarta.jms.Queue"
        ),
        @ActivationConfigProperty(
            propertyName = "acknowledgeMode",
            propertyValue = "Auto-acknowledge"
        ),
        @ActivationConfigProperty(
            propertyName = "maxSession",
            propertyValue = "5"  // Menos concurrencia que el consumidor principal
        )
    }
)
public class SincronizacionConfirmacionConsumer implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(SincronizacionConfirmacionConsumer.class.getName());

    @EJB
    private DocumentoClinicoDAO documentoDAO;

    @EJB
    private SincronizacionPendienteDAO sincronizacionDAO;

    /**
     * Callback invocado cuando llega una confirmación desde el central.
     *
     * @param message Mensaje JMS con confirmación
     */
    @Override
    public void onMessage(Message message) {

        String messageId = null;
        sincronizacion_confirmacion_message confirmacion = null;

        try {
            // Obtener ID del mensaje para trazabilidad
            messageId = message.getJMSMessageID();

            LOGGER.log(Level.INFO, "Procesando confirmación de sincronización: {0}", messageId);

            // Validar tipo de mensaje
            if (!(message instanceof ObjectMessage)) {
                LOGGER.log(Level.SEVERE, "Mensaje recibido no es ObjectMessage: {0}", message.getClass());
                throw new IllegalArgumentException("Tipo de mensaje inválido");
            }

            ObjectMessage objectMessage = (ObjectMessage) message;

            // Deserializar mensaje
            Object payload = objectMessage.getObject();
            if (!(payload instanceof sincronizacion_confirmacion_message)) {
                LOGGER.log(Level.SEVERE, "Payload no es sincronizacion_confirmacion_message: {0}",
                        payload != null ? payload.getClass() : "null");
                throw new IllegalArgumentException("Payload inválido");
            }

            confirmacion = (sincronizacion_confirmacion_message) payload;

            // Validar mensaje
            if (!confirmacion.isValid()) {
                LOGGER.log(Level.SEVERE, "Confirmación inválida: {0}", confirmacion);
                throw new IllegalArgumentException("Confirmación inválida: " + confirmacion);
            }

            LOGGER.log(Level.INFO,
                    "Confirmación recibida para documento {0}: éxito={1}, historiaId={2}",
                    new Object[]{confirmacion.getDocumentoId(), confirmacion.isExito(),
                            confirmacion.getHistoriaId()});

            // Procesar según éxito o error
            if (confirmacion.isExito()) {
                procesarConfirmacionExitosa(confirmacion);
            } else {
                procesarConfirmacionError(confirmacion);
            }

            LOGGER.log(Level.INFO,
                    "Confirmación procesada exitosamente para documento {0}",
                    confirmacion.getDocumentoId());

        } catch (IllegalArgumentException e) {
            // Error de validación - NO reintentar (mensaje envenenado)
            LOGGER.log(Level.SEVERE, "Error de validación procesando confirmación " +
                    messageId + ": " + e.getMessage(), e);
            // NO lanzar excepción - mensaje se consume y no se reintenta

        } catch (Exception e) {
            // Error inesperado - REINTENTAR
            LOGGER.log(Level.SEVERE, "Error procesando confirmación " +
                    messageId + " (se reintentará)", e);

            // Lanzar RuntimeException para provocar rollback y reintento
            throw new RuntimeException("Error procesando confirmación: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa una confirmación exitosa:
     * - Actualiza documento.hist_clinica_id
     * - Actualiza sincronizacion_pendiente.estado = RESUELTA
     */
    private void procesarConfirmacionExitosa(sincronizacion_confirmacion_message confirmacion) {

        // 1. Actualizar documento con hist_clinica_id
        Optional<documento_clinico> documentoOpt = documentoDAO.findById(confirmacion.getDocumentoId());

        if (documentoOpt.isPresent()) {
            documento_clinico documento = documentoOpt.get();

            // Verificar tenant (seguridad multi-tenancy)
            if (!confirmacion.getTenantId().equals(documento.getTenantId())) {
                LOGGER.log(Level.SEVERE,
                        "TenantId mismatch para documento {0}: confirmación={1}, documento={2}",
                        new Object[]{confirmacion.getDocumentoId(), confirmacion.getTenantId(),
                                documento.getTenantId()});
                throw new SecurityException("TenantId mismatch - posible ataque de seguridad");
            }

            // Actualizar hist_clinica_id solo si no está ya seteado (idempotencia)
            if (documento.getHistClinicaId() == null) {
                documento.setHistClinicaId(confirmacion.getHistoriaId());
                documentoDAO.save(documento);

                LOGGER.log(Level.INFO,
                        "Documento {0} actualizado con hist_clinica_id={1}",
                        new Object[]{confirmacion.getDocumentoId(), confirmacion.getHistoriaId()});
            } else {
                LOGGER.log(Level.INFO,
                        "Documento {0} ya tiene hist_clinica_id={1} (idempotencia)",
                        new Object[]{confirmacion.getDocumentoId(), documento.getHistClinicaId()});
            }

        } else {
            // Documento no encontrado - loguear pero NO fallar
            // Puede ser que se haya eliminado manualmente
            LOGGER.log(Level.WARNING,
                    "Documento {0} no encontrado para actualizar hist_clinica_id",
                    confirmacion.getDocumentoId());
        }

        // 2. Actualizar tabla de auditoría
        actualizarAuditoria(confirmacion, SincronizacionPendiente.EstadoSincronizacion.RESUELTA, null);
    }

    /**
     * Procesa una confirmación de error:
     * - Actualiza sincronizacion_pendiente.estado = ERROR
     * - Registra mensaje de error
     */
    private void procesarConfirmacionError(sincronizacion_confirmacion_message confirmacion) {

        LOGGER.log(Level.WARNING,
                "Sincronización FALLÓ para documento {0}: {1}",
                new Object[]{confirmacion.getDocumentoId(), confirmacion.getErrorMensaje()});

        // Actualizar tabla de auditoría con error
        actualizarAuditoria(confirmacion, SincronizacionPendiente.EstadoSincronizacion.ERROR,
                confirmacion.getErrorMensaje());
    }

    /**
     * Actualiza el registro de auditoría en sincronizacion_pendiente.
     *
     * @param confirmacion Confirmación recibida
     * @param estado Nuevo estado
     * @param errorMensaje Mensaje de error (null si no hay error)
     */
    private void actualizarAuditoria(
            sincronizacion_confirmacion_message confirmacion,
            SincronizacionPendiente.EstadoSincronizacion estado,
            String errorMensaje) {

        // Buscar registro de auditoría por usuario y tenant
        Optional<SincronizacionPendiente> auditoriaOpt =
                sincronizacionDAO.findByUsuarioCedulaAndTenant(
                        confirmacion.getCedula(),
                        confirmacion.getTenantId()
                );

        if (auditoriaOpt.isPresent()) {
            SincronizacionPendiente auditoria = auditoriaOpt.get();

            // Actualizar estado
            auditoria.setEstado(estado);

            if (errorMensaje != null) {
                auditoria.setUltimoError(errorMensaje);
            }

            sincronizacionDAO.save(auditoria);

            LOGGER.log(Level.INFO,
                    "Auditoría actualizada para documento {0}: estado={1}",
                    new Object[]{confirmacion.getDocumentoId(), estado});

        } else {
            // No se encontró registro de auditoría - loguear pero NO fallar
            // Puede ser que se haya limpiado la tabla
            LOGGER.log(Level.WARNING,
                    "No se encontró registro de auditoría para documento {0} (cedula={1}, tenant={2})",
                    new Object[]{confirmacion.getDocumentoId(), confirmacion.getCedula(),
                            confirmacion.getTenantId()});
        }
    }
}
