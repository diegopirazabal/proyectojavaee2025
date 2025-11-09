package hcen.central.inus.messaging;

import hcen.central.inus.dto.UsuarioSaludConfirmacionMessage;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Productor de mensajes JMS para confirmaciones de sincronización de usuarios de salud.
 *
 * Este EJB se encarga de enviar mensajes de confirmación a la cola
 * "UsuarioSaludConfirmaciones" después de procesar un usuario de salud
 * proveniente de un componente periférico.
 *
 * Características:
 * - Transaccional: Las confirmaciones se envían dentro de transacciones JTA
 * - Persistente: Los mensajes sobreviven reinicios del servidor
 * - Asíncrono: No bloquea el procesamiento del usuario
 * - Más simple que documentos: No hay historiaId ni tenantId
 *
 * @author Sistema HCEN
 * @version 1.0
 */
@Stateless
public class UsuarioSaludConfirmacionProducer {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludConfirmacionProducer.class.getName());

    /**
     * ConnectionFactory de ActiveMQ Artemis para sincronización de usuarios.
     * Configurada en configuracion-activemq-artemis.cli
     */
    @Resource(lookup = "java:/jms/UsuarioSaludConnectionFactory")
    private ConnectionFactory connectionFactory;

    /**
     * Cola JMS para confirmaciones de sincronización de usuarios.
     * Configurada en configuracion-activemq-artemis.cli
     */
    @Resource(lookup = "java:/jms/queue/UsuarioSaludConfirmaciones")
    private Queue usuarioSaludConfirmacionesQueue;

    /**
     * Envía una confirmación exitosa de sincronización de usuario.
     *
     * @param cedula Cédula del usuario registrado
     * @param messageIdOriginal ID del mensaje JMS original que se procesó
     * @return ID del mensaje de confirmación enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    public String enviarConfirmacionExitosa(
            String cedula,
            String messageIdOriginal) throws JMSException {

        LOGGER.log(Level.INFO,
                "Enviando confirmación exitosa para usuario {0}",
                cedula);

        UsuarioSaludConfirmacionMessage confirmacion =
                UsuarioSaludConfirmacionMessage.exitoso(cedula, messageIdOriginal);

        return enviarConfirmacion(confirmacion);
    }

    /**
     * Envía una confirmación de error de sincronización de usuario.
     *
     * @param cedula Cédula del usuario que falló
     * @param errorMensaje Descripción del error
     * @param messageIdOriginal ID del mensaje JMS original que se procesó
     * @return ID del mensaje de confirmación enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    public String enviarConfirmacionError(
            String cedula,
            String errorMensaje,
            String messageIdOriginal) throws JMSException {

        LOGGER.log(Level.WARNING,
                "Enviando confirmación de error para usuario {0}: {1}",
                new Object[]{cedula, errorMensaje});

        UsuarioSaludConfirmacionMessage confirmacion =
                UsuarioSaludConfirmacionMessage.fallido(cedula, errorMensaje, messageIdOriginal);

        return enviarConfirmacion(confirmacion);
    }

    /**
     * Envía un mensaje de confirmación a la cola.
     *
     * @param confirmacion Mensaje de confirmación a enviar
     * @return ID del mensaje JMS enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    private String enviarConfirmacion(UsuarioSaludConfirmacionMessage confirmacion) throws JMSException {

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
            objectMessage.setStringProperty("cedula", confirmacion.getCedula());
            objectMessage.setStringProperty("exito", String.valueOf(confirmacion.isExito()));
            objectMessage.setStringProperty("tipo", "USUARIO");

            if (confirmacion.getMessageIdOriginal() != null) {
                objectMessage.setStringProperty("messageIdOriginal", confirmacion.getMessageIdOriginal());
            }

            // Enviar mensaje a la cola
            producer.send(usuarioSaludConfirmacionesQueue, objectMessage);

            // Obtener ID del mensaje asignado por ActiveMQ
            messageId = objectMessage.getJMSMessageID();

            LOGGER.log(Level.INFO,
                    "Confirmación enviada exitosamente para usuario {0}. MessageID: {1}, Éxito: {2}",
                    new Object[]{confirmacion.getCedula(), messageId, confirmacion.isExito()});

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE,
                    "Error al enviar confirmación para usuario " + confirmacion.getCedula(), e);
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
        return connectionFactory != null && usuarioSaludConfirmacionesQueue != null;
    }
}
