package com.hcen.periferico.sync;

import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.enums.TipoSincronizacion;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación del adaptador de sincronización que publica eventos en una cola JMS.
 * El componente central consume los mensajes de la misma cola para dar de alta
 * al usuario en INUS.
 */
@Stateless
public class CentralSyncAdapterMQ implements ICentralSyncAdapter {

    private static final Logger LOGGER = Logger.getLogger(CentralSyncAdapterMQ.class.getName());
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @Resource(lookup = "java:/jms/UsuarioSaludConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "java:/jms/queue/UsuarioSaludRegistrado")
    private Queue usuarioRegistradoQueue;

    @Override
    public SyncResult enviarUsuario(UsuarioSalud usuario) {
        if (usuario == null) {
            return SyncResult.fallido("El usuario a sincronizar no puede ser nulo");
        }

        if (connectionFactory == null || usuarioRegistradoQueue == null) {
            LOGGER.severe("Recursos JMS no configurados (ConnectionFactory o Queue nulos)");
            return SyncResult.fallido("Recursos JMS no configurados en el servidor de aplicaciones");
        }

        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            String payload = buildPayload(usuario);

            JMSProducer producer = context.createProducer()
                .setProperty("evento", "UsuarioSaludRegistrado");

            if (usuario.getTenantId() != null) {
                producer.setProperty("tenantId", usuario.getTenantId().toString());
            }

            producer.send(usuarioRegistradoQueue, payload);

            LOGGER.info(() -> "Usuario encolado para sincronización con central: " + usuario.getCedula());
            return SyncResult.encolado("Usuario encolado para sincronización asíncrona");
        } catch (JMSRuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error de JMS al publicar usuario en cola", e);
            return SyncResult.fallido("Error al publicar el mensaje en la cola JMS", e.getMessage());
        }
    }

    @Override
    public boolean verificarUsuarioExiste(String cedula) {
        // No es posible verificar de forma síncrona vía mensajería.
        LOGGER.warning("verificarUsuarioExiste no soportado en adapter MQ; retornando false por defecto");
        return false;
    }

    @Override
    public String getNombre() {
        return "MQ";
    }

    @Override
    public TipoSincronizacion getTipo() {
        return TipoSincronizacion.USUARIO;
    }

    private String buildPayload(UsuarioSalud usuario) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("cedula", usuario.getCedula())
            .add("primerNombre", usuario.getPrimerNombre())
            .add("primerApellido", usuario.getPrimerApellido())
            .add("email", usuario.getEmail());

        if (usuario.getTipoDocumento() != null) {
            builder.add("tipoDocumento", usuario.getTipoDocumento());
        } else {
            builder.addNull("tipoDocumento");
        }

        if (usuario.getSegundoNombre() != null) {
            builder.add("segundoNombre", usuario.getSegundoNombre());
        } else {
            builder.addNull("segundoNombre");
        }

        if (usuario.getSegundoApellido() != null) {
            builder.add("segundoApellido", usuario.getSegundoApellido());
        } else {
            builder.addNull("segundoApellido");
        }

        if (usuario.getFechaNacimiento() != null) {
            builder.add("fechaNacimiento", ISO_DATE.format(usuario.getFechaNacimiento()));
        } else {
            builder.addNull("fechaNacimiento");
        }

        if (usuario.getTenantId() != null) {
            builder.add("tenantId", usuario.getTenantId().toString());
        } else {
            builder.addNull("tenantId");
        }

        builder.add("sincronizadoCentral", Boolean.TRUE.equals(usuario.getSincronizadoCentral()));

        if (usuario.getActive() != null) {
            builder.add("activo", usuario.getActive());
        } else {
            builder.add("activo", true);
        }

        return builder.build().toString();
    }
}
