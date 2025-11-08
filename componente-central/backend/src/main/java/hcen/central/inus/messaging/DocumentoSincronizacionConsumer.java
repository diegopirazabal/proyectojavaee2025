package hcen.central.inus.messaging;

import hcen.central.inus.dto.DocumentoSincronizacionMessage;
import hcen.central.inus.service.HistoriaClinicaService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean que consume mensajes de sincronización de documentos clínicos.
 *
 * Este MDB escucha la cola "DocumentosSincronizacion" y procesa los mensajes
 * enviados desde componentes periféricos para centralizar documentos clínicos
 * en el sistema HCEN.
 *
 * Flujo:
 * 1. Recibe mensaje con datos del documento (DocumentoSincronizacionMessage)
 * 2. Registra el documento en historia_clinica usando HistoriaClinicaService
 * 3. Envía confirmación exitosa o de error a cola "SincronizacionConfirmaciones"
 * 4. Si ocurre excepción, ActiveMQ reintentará automáticamente (max 20 veces)
 *
 * Características:
 * - Transaccional: CMT (Container Managed Transaction) por defecto
 * - Concurrente: Puede haber múltiples instancias procesando mensajes en paralelo
 * - Idempotente: Si el documento ya existe, no lo duplica
 *
 * @author Sistema HCEN
 * @version 1.0
 */
@MessageDriven(
    name = "DocumentoSincronizacionConsumer",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationLookup",
            propertyValue = "java:/jms/queue/DocumentosSincronizacion"
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
            propertyValue = "10"  // Máximo 10 instancias concurrentes procesando mensajes
        )
    }
)
public class DocumentoSincronizacionConsumer implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(DocumentoSincronizacionConsumer.class.getName());

    @EJB
    private HistoriaClinicaService historiaClinicaService;

    @EJB
    private SincronizacionConfirmacionProducer confirmacionProducer;

    /**
     * Callback invocado por el contenedor cuando llega un mensaje a la cola.
     *
     * Este método es transaccional (CMT). Si lanza una excepción:
     * - La transacción hace rollback
     * - ActiveMQ reintenta el mensaje (redelivery)
     * - Después de 20 reintentos, el mensaje va a DLQ
     *
     * @param message Mensaje JMS recibido
     */
    @Override
    public void onMessage(Message message) {

        String messageId = null;
        DocumentoSincronizacionMessage docMessage = null;

        try {
            // Obtener ID del mensaje para trazabilidad
            messageId = message.getJMSMessageID();

            LOGGER.log(Level.INFO, "Procesando mensaje de sincronización: {0}", messageId);

            // Validar tipo de mensaje
            if (!(message instanceof ObjectMessage)) {
                LOGGER.log(Level.SEVERE, "Mensaje recibido no es ObjectMessage: {0}", message.getClass());
                throw new IllegalArgumentException("Tipo de mensaje inválido");
            }

            ObjectMessage objectMessage = (ObjectMessage) message;

            // Deserializar mensaje
            Object payload = objectMessage.getObject();
            if (!(payload instanceof DocumentoSincronizacionMessage)) {
                LOGGER.log(Level.SEVERE, "Payload no es DocumentoSincronizacionMessage: {0}",
                        payload != null ? payload.getClass() : "null");
                throw new IllegalArgumentException("Payload inválido");
            }

            docMessage = (DocumentoSincronizacionMessage) payload;

            // Validar mensaje
            if (!docMessage.isValid()) {
                LOGGER.log(Level.SEVERE, "Mensaje inválido: {0}", docMessage);
                throw new IllegalArgumentException("Mensaje de sincronización inválido: " + docMessage);
            }

            LOGGER.log(Level.INFO,
                    "Sincronizando documento {0} (paciente: {1}, tenant: {2})",
                    new Object[]{docMessage.getDocumentoId(), docMessage.getCedula(), docMessage.getTenantId()});

            // Registrar documento en historia clínica central
            UUID historiaId = historiaClinicaService.registrarDocumento(
                    docMessage.getCedula(),
                    docMessage.getTenantId(),
                    docMessage.getDocumentoId()
            );

            LOGGER.log(Level.INFO,
                    "Documento {0} sincronizado exitosamente en historia {1}",
                    new Object[]{docMessage.getDocumentoId(), historiaId});

            // Enviar confirmación exitosa al periférico
            try {
                confirmacionProducer.enviarConfirmacionExitosa(
                        docMessage.getDocumentoId(),
                        historiaId,
                        docMessage.getTenantId(),
                        docMessage.getCedula(),
                        messageId
                );

                LOGGER.log(Level.INFO,
                        "Confirmación exitosa enviada para documento {0}",
                        docMessage.getDocumentoId());

            } catch (JMSException e) {
                // Error al enviar confirmación - loguear pero NO fallar transacción principal
                // El documento YA fue registrado exitosamente
                LOGGER.log(Level.SEVERE,
                        "Error al enviar confirmación para documento " + docMessage.getDocumentoId() +
                        " (documento SÍ fue sincronizado)", e);
                // No lanzar excepción - dejar que transacción se confirme
            }

        } catch (IllegalArgumentException e) {
            // Error de validación - NO reintentar (mensaje envenenado)
            LOGGER.log(Level.SEVERE, "Error de validación procesando mensaje " + messageId + ": " + e.getMessage(), e);

            // Enviar confirmación de error al periférico (si tenemos datos suficientes)
            if (docMessage != null && docMessage.getDocumentoId() != null) {
                try {
                    confirmacionProducer.enviarConfirmacionError(
                            docMessage.getDocumentoId(),
                            docMessage.getTenantId(),
                            docMessage.getCedula(),
                            "Error de validación: " + e.getMessage(),
                            messageId
                    );
                } catch (JMSException jmsEx) {
                    LOGGER.log(Level.SEVERE, "Error al enviar confirmación de error", jmsEx);
                }
            }

            // NO lanzar excepción - mensaje se consumirá y no se reintentará
            // Esto es correcto para mensajes inválidos/envenenados

        } catch (Exception e) {
            // Error inesperado - REINTENTAR (lanzar excepción para rollback)
            LOGGER.log(Level.SEVERE, "Error procesando mensaje " + messageId +
                    " (se reintentará)", e);

            // Enviar confirmación de error (best effort)
            if (docMessage != null && docMessage.getDocumentoId() != null) {
                try {
                    confirmacionProducer.enviarConfirmacionError(
                            docMessage.getDocumentoId(),
                            docMessage.getTenantId(),
                            docMessage.getCedula(),
                            "Error temporal: " + e.getMessage(),
                            messageId
                    );
                } catch (JMSException jmsEx) {
                    LOGGER.log(Level.SEVERE, "Error al enviar confirmación de error", jmsEx);
                }
            }

            // Lanzar RuntimeException para provocar rollback y reintento
            throw new RuntimeException("Error procesando documento: " + e.getMessage(), e);
        }
    }
}
