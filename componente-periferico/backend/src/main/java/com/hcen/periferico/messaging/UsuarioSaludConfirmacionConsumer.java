package com.hcen.periferico.messaging;

import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.dto.UsuarioSaludConfirmacionMessage;
import com.hcen.periferico.entity.SincronizacionPendiente;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean que consume confirmaciones de sincronización de usuarios desde el componente central.
 *
 * Este MDB escucha la cola "UsuarioSaludConfirmaciones" y procesa las confirmaciones
 * enviadas desde el componente central después de registrar un usuario de salud.
 *
 * Flujo:
 * 1. Recibe confirmación JSON (UsuarioSaludConfirmacionMessage)
 * 2. Deserializa JSON usando JSON-B
 * 3. Actualiza sincronizacion_pendiente: estado = RESUELTA (si éxito) o ERROR (si falló)
 * 4. Si excepción: ActiveMQ reintenta automáticamente
 *
 * Características:
 * - Transaccional: CMT (Container Managed Transaction)
 * - Idempotente: Puede procesar misma confirmación múltiples veces sin problemas
 * - Tolerante a fallos: Si no encuentra auditoría, loguea pero no falla
 * - Más simple que documentos: No hay ID central para actualizar en entidades
 * - Formato JSON: Procesa TextMessage con JSON-B para deserialización
 *
 * @author Sistema HCEN
 * @version 2.0 - Migrado a JSON
 */
@MessageDriven(
    name = "UsuarioSaludConfirmacionConsumer",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationLookup",
            propertyValue = "java:/jms/queue/UsuarioSaludConfirmaciones"
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
            propertyValue = "3"  // Baja concurrencia (confirmaciones son simples y poco frecuentes)
        )
    }
)
public class UsuarioSaludConfirmacionConsumer implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludConfirmacionConsumer.class.getName());

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
        UsuarioSaludConfirmacionMessage confirmacion = null;

        try (Jsonb jsonb = JsonbBuilder.create()) {
            // Obtener ID del mensaje para trazabilidad
            messageId = message.getJMSMessageID();

            LOGGER.log(Level.INFO, "Procesando confirmación JSON de usuario: {0}", messageId);

            // Validar tipo de mensaje
            if (!(message instanceof TextMessage)) {
                LOGGER.log(Level.SEVERE, "Mensaje recibido no es TextMessage: {0}", message.getClass());
                throw new IllegalArgumentException("Tipo de mensaje inválido, se esperaba TextMessage");
            }

            TextMessage textMessage = (TextMessage) message;

            // Obtener JSON del mensaje
            String jsonPayload = textMessage.getText();
            if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
                LOGGER.log(Level.SEVERE, "Mensaje recibido sin contenido JSON");
                throw new IllegalArgumentException("Mensaje vacío");
            }

            LOGGER.log(Level.FINE, "JSON confirmación recibido: {0}", jsonPayload);

            // Deserializar JSON a DTO
            try {
                confirmacion = jsonb.fromJson(jsonPayload, UsuarioSaludConfirmacionMessage.class);
            } catch (JsonbException e) {
                LOGGER.log(Level.SEVERE, "Error al deserializar JSON de confirmación: " + jsonPayload, e);
                throw new IllegalArgumentException("JSON inválido: " + e.getMessage(), e);
            }

            // Validar mensaje
            if (!confirmacion.isValid()) {
                LOGGER.log(Level.SEVERE, "Confirmación inválida: {0}", confirmacion);
                throw new IllegalArgumentException("Confirmación inválida: " + confirmacion);
            }

            LOGGER.log(Level.INFO,
                    "Confirmación recibida para usuario {0}: éxito={1}",
                    new Object[]{confirmacion.getCedula(), confirmacion.isExito()});

            // Procesar según éxito o error
            if (confirmacion.isExito()) {
                procesarConfirmacionExitosa(confirmacion);
            } else {
                procesarConfirmacionError(confirmacion);
            }

            LOGGER.log(Level.INFO,
                    "Confirmación procesada exitosamente para usuario {0}",
                    confirmacion.getCedula());

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
     * - Actualiza sincronizacion_pendiente.estado = RESUELTA
     */
    private void procesarConfirmacionExitosa(UsuarioSaludConfirmacionMessage confirmacion) {

        LOGGER.log(Level.INFO,
                "Sincronización EXITOSA para usuario {0}",
                confirmacion.getCedula());

        // Actualizar tabla de auditoría
        actualizarAuditoria(confirmacion, SincronizacionPendiente.EstadoSincronizacion.RESUELTA, null);
    }

    /**
     * Procesa una confirmación de error:
     * - Actualiza sincronizacion_pendiente.estado = ERROR
     * - Registra mensaje de error
     */
    private void procesarConfirmacionError(UsuarioSaludConfirmacionMessage confirmacion) {

        LOGGER.log(Level.WARNING,
                "Sincronización FALLÓ para usuario {0}: {1}",
                new Object[]{confirmacion.getCedula(), confirmacion.getErrorMensaje()});

        // Actualizar tabla de auditoría con error
        actualizarAuditoria(confirmacion, SincronizacionPendiente.EstadoSincronizacion.ERROR,
                confirmacion.getErrorMensaje());
    }

    /**
     * Actualiza el registro de auditoría en sincronizacion_pendiente.
     *
     * NOTA: Busca por cedula únicamente (sin tenant_id), ya que el central
     * no maneja tenant_id para usuarios. Si hay múltiples registros de auditoría
     * para diferentes tenants con la misma cedula, se actualizarán todos.
     *
     * @param confirmacion Confirmación recibida
     * @param estado Nuevo estado
     * @param errorMensaje Mensaje de error (null si no hay error)
     */
    private void actualizarAuditoria(
            UsuarioSaludConfirmacionMessage confirmacion,
            SincronizacionPendiente.EstadoSincronizacion estado,
            String errorMensaje) {

        // Buscar registros de auditoría por cedula de usuario
        // NOTA: Puede haber múltiples registros (uno por tenant) ya que el periférico
        // es multi-tenant pero el central no. Actualizamos el más reciente.
        java.util.List<SincronizacionPendiente> auditorias =
                sincronizacionDAO.findByUsuarioCedula(confirmacion.getCedula());

        if (auditorias.isEmpty()) {
            // No se encontró registro de auditoría - loguear pero NO fallar
            // Puede ser que se haya limpiado la tabla o que sea un mensaje duplicado antiguo
            LOGGER.log(Level.WARNING,
                    "No se encontró registro de auditoría para usuario {0}",
                    confirmacion.getCedula());
            return;
        }

        // Actualizar el registro más reciente (último enviado)
        SincronizacionPendiente auditoriaMasReciente = auditorias.stream()
                .max((a, b) -> a.getFecEnvioCola().compareTo(b.getFecEnvioCola()))
                .orElse(auditorias.get(0));

        // Actualizar estado
        auditoriaMasReciente.setEstado(estado);

        if (errorMensaje != null) {
            auditoriaMasReciente.setUltimoError(errorMensaje);
        }

        sincronizacionDAO.save(auditoriaMasReciente);

        LOGGER.log(Level.INFO,
                "Auditoría actualizada para usuario {0}: estado={1}",
                new Object[]{confirmacion.getCedula(), estado});
    }
}
