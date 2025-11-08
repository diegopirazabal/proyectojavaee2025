package com.hcen.periferico.messaging;

import hcen.central.inus.dto.DocumentoSincronizacionMessage;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Productor de mensajes JMS para sincronización de documentos clínicos.
 *
 * Este EJB se encarga de enviar mensajes a la cola "DocumentosSincronizacion"
 * cuando se crea un nuevo documento clínico que debe ser centralizado en el
 * componente central.
 *
 * Características:
 * - Transaccional: Los mensajes se envían dentro de transacciones JTA
 * - Persistente: Los mensajes sobreviven reinicios del servidor
 * - Asíncrono: No bloquea la creación del documento
 *
 * @author Sistema HCEN
 * @version 1.0
 */
@Stateless
public class DocumentoSincronizacionProducer {

    private static final Logger LOGGER = Logger.getLogger(DocumentoSincronizacionProducer.class.getName());

    /**
     * ConnectionFactory de ActiveMQ Artemis.
     * WildFly proporciona esta factory por defecto.
     */
    @Resource(lookup = "java:/jms/DocumentosSyncCF")
    private ConnectionFactory connectionFactory;

    /**
     * Cola JMS para sincronización de documentos.
     * Configurada en activemq-jms.xml
     */
    @Resource(lookup = "java:/jms/queue/DocumentosSincronizacion")
    private Queue documentosSincronizacionQueue;

    /**
     * Envía un documento a la cola de sincronización.
     *
     * Este método es transaccional (CMT - Container Managed Transaction).
     * Si la transacción falla (rollback), el mensaje NO se enviará.
     *
     * @param documentoId ID del documento a sincronizar
     * @param cedula Cédula del paciente
     * @param tenantId ID de la clínica
     * @return ID del mensaje JMS enviado
     * @throws JMSException si ocurre error al enviar mensaje
     */
    public String enviarDocumento(UUID documentoId, String cedula, UUID tenantId) throws JMSException {

        LOGGER.log(Level.INFO, "Enviando documento {0} a cola de sincronización (tenant: {1}, cedula: {2})",
                new Object[]{documentoId, tenantId, cedula});

        // Validar parámetros
        if (documentoId == null || cedula == null || tenantId == null) {
            throw new IllegalArgumentException("documentoId, cedula y tenantId son requeridos");
        }

        // Crear mensaje DTO
        DocumentoSincronizacionMessage mensaje = new DocumentoSincronizacionMessage(
                documentoId,
                tenantId,
                cedula
        );

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
            objectMessage.setStringProperty("tenantId", tenantId.toString());
            objectMessage.setStringProperty("cedula", cedula);
            objectMessage.setStringProperty("documentoId", documentoId.toString());
            objectMessage.setStringProperty("tipo", "DOCUMENTO");

            // Enviar mensaje a la cola
            producer.send(documentosSincronizacionQueue, objectMessage);

            // Obtener ID del mensaje asignado por ActiveMQ
            messageId = objectMessage.getJMSMessageID();

            LOGGER.log(Level.INFO, "Documento {0} enviado exitosamente a cola. MessageID: {1}",
                    new Object[]{documentoId, messageId});

        } catch (JMSRuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al enviar documento " + documentoId + " a cola de sincronización", e);
            JMSException jmsException = new JMSException("Error al enviar documento a cola JMS");
            jmsException.setLinkedException(e);
            throw jmsException; // Propagar excepción para rollback de transacción
        }

        return messageId;
    }

    /**
     * Envía múltiples documentos a la cola de sincronización.
     *
     * Útil para reintentos batch o migraciones.
     *
     * @param mensajes Lista de mensajes a enviar
     * @return Número de mensajes enviados exitosamente
     */
    public int enviarDocumentos(java.util.List<DocumentoSincronizacionMessage> mensajes) {

        int enviados = 0;

        try (JMSContext context = connectionFactory.createContext()) {

            JMSProducer producer = context.createProducer();
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setPriority(4);

            for (DocumentoSincronizacionMessage mensaje : mensajes) {

                if (!mensaje.isValid()) {
                    LOGGER.log(Level.WARNING, "Mensaje inválido omitido: {0}", mensaje);
                    continue;
                }

                try {
                    ObjectMessage objectMessage = context.createObjectMessage(mensaje);

                    objectMessage.setStringProperty("tenantId", mensaje.getTenantId().toString());
                    objectMessage.setStringProperty("cedula", mensaje.getCedula());
                    objectMessage.setStringProperty("documentoId", mensaje.getDocumentoId().toString());
                    objectMessage.setStringProperty("tipo", "DOCUMENTO");

                    producer.send(documentosSincronizacionQueue, objectMessage);

                    enviados++;

                } catch (JMSException e) {
                    LOGGER.log(Level.SEVERE, "Error al enviar mensaje: " + mensaje, e);
                    // Continuar con siguiente mensaje
                }
            }

        } catch (JMSRuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al crear contexto JMS para envío batch", e);
        }

        LOGGER.log(Level.INFO, "Enviados {0} de {1} documentos a cola de sincronización",
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
        return connectionFactory != null && documentosSincronizacionQueue != null;
    }
}
