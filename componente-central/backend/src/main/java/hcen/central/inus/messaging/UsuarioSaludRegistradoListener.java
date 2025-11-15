package hcen.central.inus.messaging;

import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.dto.UsuarioSaludSincronizacionMessage;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.service.UsuarioSaludService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message-Driven Bean que consume mensajes de sincronización de usuarios de salud.
 *
 * Este MDB escucha la cola "UsuarioSaludRegistrado" y procesa los mensajes
 * enviados desde componentes periféricos para registrar usuarios de salud
 * en el sistema nacional HCEN.
 *
 * Flujo:
 * 1. Recibe mensaje JSON con datos del usuario (UsuarioSaludSincronizacionMessage)
 * 2. Deserializa JSON usando JSON-B
 * 3. Registra el usuario usando UsuarioSaludService (idempotente)
 * 4. Envía confirmación exitosa o de error a cola "UsuarioSaludConfirmaciones"
 * 5. Si ocurre excepción, ActiveMQ reintentará automáticamente (max 5 veces)
 *
 * Características:
 * - Transaccional: CMT (Container Managed Transaction) por defecto
 * - Concurrente: Puede haber múltiples instancias procesando mensajes en paralelo (max 5)
 * - Idempotente: Si el usuario ya existe, devuelve el existente sin error
 * - Sin tenant_id: El central NO maneja multi-tenancy para usuarios
 * - Formato JSON: Procesa TextMessage con JSON-B para deserialización
 *
 * @author Sistema HCEN
 * @version 2.0 - Migrado a JSON
 */
@MessageDriven(
    name = "UsuarioSaludRegistradoListener",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationLookup",
            propertyValue = "java:/jms/queue/UsuarioSaludRegistrado"
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
            propertyValue = "5"  // Máximo 5 instancias concurrentes (usuarios son menos frecuentes que documentos)
        )
    }
)
public class UsuarioSaludRegistradoListener implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludRegistradoListener.class.getName());

    @EJB
    private UsuarioSaludService usuarioSaludService;

    @EJB
    private UsuarioSaludConfirmacionProducer confirmacionProducer;

    /**
     * Callback invocado por el contenedor cuando llega un mensaje a la cola.
     *
     * Este método es transaccional (CMT). Si lanza una excepción:
     * - La transacción hace rollback
     * - ActiveMQ reintenta el mensaje (redelivery)
     * - Después de 5 reintentos, el mensaje va a DLQ
     *
     * @param message Mensaje JMS recibido
     */
    @Override
    public void onMessage(Message message) {

        String messageId = null;
        UsuarioSaludSincronizacionMessage userMessage = null;

        try (Jsonb jsonb = JsonbBuilder.create()) {
            // Obtener ID del mensaje para trazabilidad
            messageId = message.getJMSMessageID();

            LOGGER.log(Level.INFO, "Procesando mensaje JSON de usuario: {0}", messageId);

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

            LOGGER.log(Level.FINE, "JSON recibido: {0}", jsonPayload);

            // Deserializar JSON a DTO
            try {
                userMessage = jsonb.fromJson(jsonPayload, UsuarioSaludSincronizacionMessage.class);
            } catch (JsonbException e) {
                LOGGER.log(Level.SEVERE, "Error al deserializar JSON: " + jsonPayload, e);
                throw new IllegalArgumentException("JSON inválido: " + e.getMessage(), e);
            }

            // Validar mensaje
            if (!userMessage.isValid()) {
                LOGGER.log(Level.SEVERE, "Mensaje inválido: {0}", userMessage);
                throw new IllegalArgumentException("Mensaje de sincronización inválido: " + userMessage);
            }

            LOGGER.log(Level.INFO,
                    "Sincronizando usuario {0} (tipo: {1})",
                    new Object[]{userMessage.getCedula(), userMessage.getTipoDocumento()});

            // Convertir String a enum TipoDocumento
            TipoDocumento tipoDocEnum;
            try {
                tipoDocEnum = TipoDocumento.valueOf(userMessage.getTipoDocumento());
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Tipo de documento inválido: {0}", userMessage.getTipoDocumento());
                throw new IllegalArgumentException("Tipo de documento no válido: " + userMessage.getTipoDocumento());
            }

            // Registrar usuario en sistema nacional
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest();
            request.setCedula(userMessage.getCedula());
            request.setTipoDocumento(tipoDocEnum);

            UsuarioSaludDTO usuarioDTO = usuarioSaludService.registrarUsuarioEnClinica(request);

            LOGGER.log(Level.INFO,
                    "Usuario {0} sincronizado exitosamente (ID: {1})",
                    new Object[]{userMessage.getCedula(), usuarioDTO.getId()});

            // Enviar confirmación exitosa al periférico
            try {
                confirmacionProducer.enviarConfirmacionExitosa(
                        userMessage.getCedula(),
                        messageId
                );

                LOGGER.log(Level.INFO,
                        "Confirmación exitosa enviada para usuario {0}",
                        userMessage.getCedula());

            } catch (JMSException e) {
                // Error al enviar confirmación - loguear pero NO fallar transacción principal
                // El usuario YA fue registrado exitosamente
                LOGGER.log(Level.SEVERE,
                        "Error al enviar confirmación para usuario " + userMessage.getCedula() +
                        " (usuario SÍ fue sincronizado)", e);
                // No lanzar excepción - dejar que transacción se confirme
            }

        } catch (IllegalArgumentException e) {
            // Error de validación - NO reintentar (mensaje envenenado)
            LOGGER.log(Level.SEVERE, "Error de validación procesando mensaje " + messageId + ": " + e.getMessage(), e);

            // Enviar confirmación de error al periférico (si tenemos datos suficientes)
            if (userMessage != null && userMessage.getCedula() != null) {
                try {
                    confirmacionProducer.enviarConfirmacionError(
                            userMessage.getCedula(),
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
            if (userMessage != null && userMessage.getCedula() != null) {
                try {
                    confirmacionProducer.enviarConfirmacionError(
                            userMessage.getCedula(),
                            "Error temporal: " + e.getMessage(),
                            messageId
                    );
                } catch (JMSException jmsEx) {
                    LOGGER.log(Level.SEVERE, "Error al enviar confirmación de error", jmsEx);
                }
            }

            // Lanzar RuntimeException para provocar rollback y reintento
            throw new RuntimeException("Error procesando usuario: " + e.getMessage(), e);
        }
    }
}
