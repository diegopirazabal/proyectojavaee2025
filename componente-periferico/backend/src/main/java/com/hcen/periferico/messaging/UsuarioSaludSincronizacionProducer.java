package com.hcen.periferico.messaging;

import hcen.central.inus.dto.UsuarioSaludSincronizacionMessage;
import com.hcen.periferico.enums.TipoDocumento;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Productor de mensajes JMS para sincronización de usuarios de salud.
 *
 * Este EJB se encarga de enviar mensajes a la cola "UsuarioSaludRegistrado"
 * cuando se registra un nuevo usuario de salud que debe ser sincronizado con
 * el componente central.
 *
 * Características:
 * - Transaccional: Los mensajes se envían dentro de transacciones JTA
 * - Persistente: Los mensajes sobreviven reinicios del servidor
 * - Asíncrono: No bloquea el registro del usuario
 * - Idempotente: El central maneja duplicados sin error
 *
 * @author Sistema HCEN
 * @version 1.0
 */
@Stateless
public class UsuarioSaludSincronizacionProducer {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludSincronizacionProducer.class.getName());

    /**
     * ConnectionFactory de ActiveMQ Artemis para sincronización de usuarios.
     * Configurada en configuracion-activemq-artemis.cli
     */
    @Resource(lookup = "java:/jms/UsuarioSaludConnectionFactory")
    private ConnectionFactory connectionFactory;

    /**
     * Cola JMS para sincronización de usuarios de salud.
     * Configurada en configuracion-activemq-artemis.cli
     */
    @Resource(lookup = "java:/jms/queue/UsuarioSaludRegistrado")
    private Queue usuarioSaludRegistradoQueue;

    /**
     * Envía un usuario de salud a la cola de sincronización.
     *
     * Este método es transaccional (CMT - Container Managed Transaction).
     * Si la transacción falla (rollback), el mensaje NO se enviará.
     *
     * @param cedula Cédula del usuario de salud
     * @param tipoDocumento Tipo de documento del usuario
     * @return ID del mensaje JMS enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    public String enviarUsuario(String cedula, TipoDocumento tipoDocumento) throws JMSException {

        LOGGER.log(Level.INFO, "Enviando usuario {0} ({1}) a cola de sincronización",
                new Object[]{cedula, tipoDocumento});

        // Validar parámetros
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("cedula es requerida");
        }
        if (tipoDocumento == null) {
            throw new IllegalArgumentException("tipoDocumento es requerido");
        }

        // Crear mensaje DTO (convirtiendo enum a String)
        UsuarioSaludSincronizacionMessage mensaje = new UsuarioSaludSincronizacionMessage(
                cedula.trim(),
                tipoDocumento.name()  // Convertir enum a String
        );

        // Validar mensaje
        if (!mensaje.isValid()) {
            throw new IllegalArgumentException("Mensaje de sincronización inválido: " + mensaje);
        }

        String messageId = null;

        try (JMSContext context = connectionFactory.createContext()) {

            // Crear productor JMS
            JMSProducer producer = context.createProducer();

            // Configurar mensaje como PERSISTENTE
            // Esto garantiza que el mensaje sobreviva reinicios del broker
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // Establecer prioridad normal (4 = default)
            producer.setPriority(4);

            // Crear ObjectMessage con el DTO
            ObjectMessage objectMessage = context.createObjectMessage(mensaje);

            // Agregar propiedades personalizadas para filtrado/monitoreo
            objectMessage.setStringProperty("cedula", cedula);
            objectMessage.setStringProperty("tipoDocumento", tipoDocumento.name());
            objectMessage.setStringProperty("tipo", "USUARIO");

            // Enviar mensaje a la cola
            producer.send(usuarioSaludRegistradoQueue, objectMessage);

            // Obtener ID del mensaje asignado por ActiveMQ
            messageId = objectMessage.getJMSMessageID();

            LOGGER.log(Level.INFO, "Usuario {0} enviado exitosamente a cola. MessageID: {1}",
                    new Object[]{cedula, messageId});

        } catch (JMSRuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al enviar usuario " + cedula + " a cola de sincronización", e);
            JMSException jmsException = new JMSException("Error al enviar usuario a cola JMS");
            jmsException.setLinkedException(e);
            throw jmsException; // Propagar excepción para rollback de transacción
        }

        return messageId;
    }

    /**
     * Envía múltiples usuarios a la cola de sincronización.
     *
     * Útil para reintentos batch o migraciones.
     *
     * @param mensajes Lista de mensajes a enviar
     * @return Número de mensajes enviados exitosamente
     */
    public int enviarUsuarios(java.util.List<UsuarioSaludSincronizacionMessage> mensajes) {

        int enviados = 0;

        try (JMSContext context = connectionFactory.createContext()) {

            JMSProducer producer = context.createProducer();
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setPriority(4);

            for (UsuarioSaludSincronizacionMessage mensaje : mensajes) {

                if (!mensaje.isValid()) {
                    LOGGER.log(Level.WARNING, "Mensaje inválido omitido: {0}", mensaje);
                    continue;
                }

                try {
                    ObjectMessage objectMessage = context.createObjectMessage(mensaje);

                    objectMessage.setStringProperty("cedula", mensaje.getCedula());
                    objectMessage.setStringProperty("tipoDocumento", mensaje.getTipoDocumento());  // Ya es String
                    objectMessage.setStringProperty("tipo", "USUARIO");

                    producer.send(usuarioSaludRegistradoQueue, objectMessage);

                    enviados++;

                } catch (JMSException e) {
                    LOGGER.log(Level.SEVERE, "Error al enviar mensaje: " + mensaje, e);
                    // Continuar con siguiente mensaje
                }
            }

        } catch (JMSRuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al crear contexto JMS para envío batch", e);
        }

        LOGGER.log(Level.INFO, "Enviados {0} de {1} usuarios a cola de sincronización",
                new Object[]{enviados, mensajes.size()});

        return enviados;
    }

    /**
     * Verifica que las dependencias JMS estén disponibles.
     *
     * Útil para health checks y diagnóstico.
     *
     * @return true si el productor está configurado correctamente
     */
    public boolean isConfigured() {
        return connectionFactory != null && usuarioSaludRegistradoQueue != null;
    }
}
