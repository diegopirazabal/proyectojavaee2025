package hcen.central.inus.messaging;

import hcen.central.inus.dto.SincronizacionConfirmacionMessage;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Productor de mensajes JMS para confirmaciones de sincronización.
 *
 * Este EJB se encarga de enviar mensajes de confirmación a la cola
 * "SincronizacionConfirmaciones" después de procesar un documento clínico
 * proveniente de un componente periférico.
 *
 * Características:
 * - Transaccional: Las confirmaciones se envían dentro de transacciones JTA
 * - Persistente: Los mensajes sobreviven reinicios del servidor
 * - Asíncrono: No bloquea el procesamiento del documento
 *
 * @author Sistema HCEN
 * @version 1.0
 */
@Stateless
public class SincronizacionConfirmacionProducer {

    private static final Logger LOGGER = Logger.getLogger(SincronizacionConfirmacionProducer.class.getName());

    /**
     * ConnectionFactory de ActiveMQ Artemis.
     * WildFly proporciona esta factory.
     */
    @Resource(lookup = "java:/jms/DocumentosSyncCF")
    private ConnectionFactory connectionFactory;

    /**
     * Cola JMS para confirmaciones de sincronización.
     * Configurada en activemq-jms.xml
     */
    @Resource(lookup = "java:/jms/queue/SincronizacionConfirmaciones")
    private Queue sincronizacionConfirmacionesQueue;

    /**
     * Envía una confirmación exitosa de sincronización.
     *
     * @param documentoId ID del documento sincronizado
     * @param historiaId ID de la historia clínica central
     * @param tenantId ID de la clínica
     * @param cedula Cédula del paciente
     * @param messageIdOriginal ID del mensaje JMS original que se procesó
     * @return ID del mensaje de confirmación enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    public String enviarConfirmacionExitosa(
            UUID documentoId,
            UUID historiaId,
            UUID tenantId,
            String cedula,
            String messageIdOriginal) throws JMSException {

        LOGGER.log(Level.INFO,
                "Enviando confirmación exitosa para documento {0} (historia: {1}, tenant: {2})",
                new Object[]{documentoId, historiaId, tenantId});

        SincronizacionConfirmacionMessage confirmacion =
                SincronizacionConfirmacionMessage.exitoso(documentoId, historiaId, tenantId, cedula);
        confirmacion.setMessageIdOriginal(messageIdOriginal);

        return enviarConfirmacion(confirmacion);
    }

    /**
     * Envía una confirmación de error de sincronización.
     *
     * @param documentoId ID del documento que falló
     * @param tenantId ID de la clínica
     * @param cedula Cédula del paciente
     * @param errorMensaje Descripción del error
     * @param messageIdOriginal ID del mensaje JMS original que se procesó
     * @return ID del mensaje de confirmación enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    public String enviarConfirmacionError(
            UUID documentoId,
            UUID tenantId,
            String cedula,
            String errorMensaje,
            String messageIdOriginal) throws JMSException {

        LOGGER.log(Level.WARNING,
                "Enviando confirmación de error para documento {0} (tenant: {1}): {2}",
                new Object[]{documentoId, tenantId, errorMensaje});

        SincronizacionConfirmacionMessage confirmacion =
                SincronizacionConfirmacionMessage.fallido(documentoId, tenantId, cedula, errorMensaje);
        confirmacion.setMessageIdOriginal(messageIdOriginal);

        return enviarConfirmacion(confirmacion);
    }

    /**
     * Envía un mensaje de confirmación a la cola.
     *
     * @param confirmacion Mensaje de confirmación a enviar
     * @return ID del mensaje JMS enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    private String enviarConfirmacion(SincronizacionConfirmacionMessage confirmacion) throws JMSException {

        // Validar mensaje
        if (!confirmacion.isValid()) {
            throw new IllegalArgumentException("Mensaje de confirmación inválido: " + confirmacion);
        }

        String messageId = null;

        try (JMSContext context = connectionFactory.createContext()) {

            // Crear productor JMS
            JMSProducer producer = context.createProducer();

            // Configurar mensaje como PERSISTENTE
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // Prioridad normal
            producer.setPriority(4);

            // Crear ObjectMessage con el DTO
            ObjectMessage objectMessage = context.createObjectMessage(confirmacion);

            // Agregar propiedades personalizadas para filtrado/monitoreo
            objectMessage.setStringProperty("tenantId", confirmacion.getTenantId().toString());
            objectMessage.setStringProperty("cedula", confirmacion.getCedula());
            objectMessage.setStringProperty("documentoId", confirmacion.getDocumentoId().toString());
            objectMessage.setStringProperty("exito", String.valueOf(confirmacion.isExito()));

            if (confirmacion.getMessageIdOriginal() != null) {
                objectMessage.setStringProperty("messageIdOriginal", confirmacion.getMessageIdOriginal());
            }

            // Enviar mensaje a la cola
            producer.send(sincronizacionConfirmacionesQueue, objectMessage);

            // Obtener ID del mensaje asignado por ActiveMQ
            messageId = objectMessage.getJMSMessageID();

            LOGGER.log(Level.INFO,
                    "Confirmación enviada exitosamente para documento {0}. MessageID: {1}, Éxito: {2}",
                    new Object[]{confirmacion.getDocumentoId(), messageId, confirmacion.isExito()});

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE,
                    "Error al enviar confirmación para documento " + confirmacion.getDocumentoId(), e);
            throw e; // Propagar excepción para rollback de transacción
        }

        return messageId;
    }

    /**
     * Verifica que las dependencias JMS estén disponibles.
     *
     * @return true si el productor está configurado correctamente
     */
    public boolean isConfigured() {
        return connectionFactory != null && sincronizacionConfirmacionesQueue != null;
    }
}
