package hcen.central.inus.messaging;

import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.service.UsuarioSaludService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message Driven Bean que consume eventos de registro de usuarios enviados desde el componente periférico.
 * Cada mensaje genera (o confirma) el alta del usuario en INUS.
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/UsuarioSaludRegistrado"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "java:/jms/UsuarioSaludConnectionFactory"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class UsuarioSaludRegistradoListener implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludRegistradoListener.class.getName());

    @EJB
    private UsuarioSaludService usuarioSaludService;

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage textMessage)) {
            LOGGER.warning("Se recibió un mensaje JMS que no es TextMessage; se descarta");
            return;
        }

        try {
            String payload = textMessage.getText();
            JsonObject json = parsePayload(payload);
            RegistrarUsuarioRequest request = buildRequest(json);

            usuarioSaludService.registrarUsuarioEnClinica(request);
            LOGGER.info(() -> "Usuario registrado/actualizado via cola JMS: " + request.getCedula());
        } catch (IllegalArgumentException ex) {
            // El servicio arrojará IllegalArgumentException para duplicados u otras validaciones.
            // No queremos reintento infinito para estos casos.
            LOGGER.log(Level.INFO, "Registro omitido para mensaje JMS: {0}", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error procesando mensaje JMS de usuario salud", ex);
            throw new RuntimeException("Error al procesar mensaje JMS de usuario salud", ex);
        }
    }

    private JsonObject parsePayload(String payload) {
        try (JsonReader reader = Json.createReader(new StringReader(payload))) {
            return reader.readObject();
        }
    }

    private RegistrarUsuarioRequest buildRequest(JsonObject json) {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest();
        request.setCedula(json.getString("cedula"));

        if (!json.isNull("tipoDocumento") && !json.getString("tipoDocumento").isBlank()) {
            request.setTipoDocumento(parseTipoDocumento(json.getString("tipoDocumento")));
        } else {
            request.setTipoDocumento(TipoDocumento.DO);
        }

        request.setPrimerNombre(json.getString("primerNombre"));
        request.setPrimerApellido(json.getString("primerApellido"));
        request.setEmail(json.getString("email"));

        request.setSegundoNombre(readOptional(json, "segundoNombre"));
        request.setSegundoApellido(readOptional(json, "segundoApellido"));

        String fechaNacimiento = readOptional(json, "fechaNacimiento");
        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) {
            request.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
        }

        String tenantId = readOptional(json, "tenantId");
        if (tenantId != null && !tenantId.isBlank()) {
            request.setTenantId(UUID.fromString(tenantId));
        }

        return request;
    }

    private TipoDocumento parseTipoDocumento(String value) {
        try {
            return TipoDocumento.valueOf(value);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Tipo de documento inválido recibido en JMS ({0}); se usará DO", value);
            return TipoDocumento.DO;
        }
    }

    private String readOptional(JsonObject json, String key) {
        if (!json.containsKey(key) || json.isNull(key)) {
            return null;
        }
        String value = json.getString(key, null);
        return value != null && !value.isBlank() ? value : null;
    }
}
