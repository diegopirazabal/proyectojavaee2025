package hcen.central.inus.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.gson.Gson;
import hcen.central.inus.config.FirebaseInitializer;
import hcen.central.inus.dao.NotificacionDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.SolicitudAccesoNotificacionDTO;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.notificacion;
import hcen.central.notifications.entity.FCMToken;
import hcen.central.notifications.service.fcm_token_service;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class notification_service {

    private static final Logger LOGGER = Logger.getLogger(notification_service.class.getName());
    private static final String DEFAULT_TOPIC = "all-users";
    private static final String USER_TOPIC_PREFIX = "user-";

    @Inject
    private FirebaseInitializer firebaseInitializer;

    @EJB
    private fcm_token_service fcmTokenService;

    @EJB
    private NotificacionDAO notificacionDAO;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    private final Gson gson = new Gson();

    public void sendBroadcastTestNotification() {
        if (!firebaseInitializer.isReady()) {
            LOGGER.warning("Firebase not initialized; skipping broadcast test notification.");
            return;
        }

        // Obtener todos los tokens activos del sistema
        List<FCMToken> activeTokens = fcmTokenService.getAllActiveTokens();

        if (activeTokens.isEmpty()) {
            LOGGER.warning("No hay tokens FCM activos registrados. No se enviarán notificaciones.");
            return;
        }

        LOGGER.info("Enviando notificación broadcast a " + activeTokens.size() + " dispositivos");
        Map<Long, Boolean> permisosNotificaciones = new HashMap<>();

        // Crear notificación
        Notification notification = Notification.builder()
                .setTitle("HCEN - Notificación de Prueba")
                .setBody("Esta es una notificación de prueba enviada a todos los usuarios desde el Admin HCEN")
                .build();

        FirebaseMessaging messaging = firebaseInitializer.getMessaging();
        int successCount = 0;
        int failureCount = 0;

        // Enviar notificación a cada token individualmente
        for (FCMToken token : activeTokens) {
            Long usuarioId = token.getUsuarioId();
            if (usuarioId == null) {
                LOGGER.fine(() -> "Token " + token.getId() + " no tiene usuario asociado. Omitiendo.");
                continue;
            }

            Boolean puedeRecibir = permisosNotificaciones.get(usuarioId);
            if (puedeRecibir == null) {
                Optional<UsuarioSalud> usuarioOpt = usuarioSaludDAO.findById(usuarioId);
                if (usuarioOpt.isEmpty()) {
                    LOGGER.fine(() -> "No se encontró usuario " + usuarioId + " para token " + token.getId());
                    permisosNotificaciones.put(usuarioId, Boolean.FALSE);
                    continue;
                }
                puedeRecibir = puedeRecibirNotificaciones(usuarioOpt.get());
                permisosNotificaciones.put(usuarioId, puedeRecibir);
            }

            if (!puedeRecibir) {
                LOGGER.fine(() -> "Usuario " + usuarioId + " tiene notificaciones deshabilitadas. Token " +
                        token.getId() + " omitido.");
                continue;
            }

            try {
                Message message = Message.builder()
                        .setToken(token.getFcmToken())
                        .setNotification(notification)
                        .putData("tipo", "SISTEMA")
                        .putData("mensaje", "Notificación de prueba")
                        .putData("timestamp", Instant.now().toString())
                        .putData("usuarioId", String.valueOf(token.getUsuarioId()))
                        .build();

                String messageId = messaging.send(message);
                successCount++;
                LOGGER.fine("Notificación enviada a token ID " + token.getId() + ". Message ID: " + messageId);

            } catch (Exception e) {
                failureCount++;
                LOGGER.log(Level.WARNING,
                    "Error al enviar notificación a token ID " + token.getId() +
                    " (usuario " + token.getUsuarioId() + "): " + e.getMessage(), e);
            }
        }

        LOGGER.info(String.format(
            "Notificación broadcast completada. Éxitos: %d, Fallos: %d, Total: %d",
            successCount, failureCount, activeTokens.size()
        ));
    }

    public boolean sendDirectNotificationToUser(String cedula, String body) {
        if (!firebaseInitializer.isReady()) {
            LOGGER.warning("Firebase not initialized; skipping direct notification.");
            return false;
        }

        Optional<UsuarioSalud> usuarioOpt = usuarioSaludDAO.findByCedula(cedula);
        if (usuarioOpt.isEmpty()) {
            LOGGER.warning("Intento de notificar a cédula " + cedula + " que no está registrada.");
            return false;
        }
        if (!puedeRecibirNotificaciones(usuarioOpt.get())) {
            LOGGER.info(() -> "Usuario " + cedula + " tiene notificaciones deshabilitadas. Se omite el envío push.");
            return true;
        }

        String sanitizedCedula = sanitizeTopicSegment(cedula);
        String topic = USER_TOPIC_PREFIX + sanitizedCedula;
        String messageBody = (body == null || body.isBlank()) ? "mensaje de prueba" : body;

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle("HCEN")
                        .setBody(messageBody)
                        .build())
                .putData("cedula", sanitizedCedula)
                .build();

        try {
            FirebaseMessaging messaging = firebaseInitializer.getMessaging();
            String messageId = messaging.send(message);
            LOGGER.info(() -> "Direct notification sent to topic " + topic + ". Message ID: " + messageId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send direct notification to user topic " + topic, e);
            return false;
        }
    }

    private String sanitizeTopicSegment(String value) {
        if (value == null || value.isBlank()) {
            return "desconocido";
        }
        return value.replaceAll("[^A-Za-z0-9-_.~]", "-");
    }

    /**
     * Envía una notificación de solicitud de acceso a documento clínico
     * El profesional solicita permiso para ver un documento del paciente
     * TAMBIÉN guarda la notificación en la base de datos con estado PENDIENTE
     *
     * @param solicitud DTO con toda la información de la solicitud
     * @return true si la notificación fue enviada exitosamente
     */
    public boolean enviarNotificacionSolicitudAcceso(SolicitudAccesoNotificacionDTO solicitud) {
        try {
            // 1. Guardar la notificación en base de datos
            Optional<UsuarioSalud> usuarioOpt = usuarioSaludDAO.findByCedula(solicitud.getCedulaPaciente());
            if (usuarioOpt.isEmpty()) {
                LOGGER.warning("No se encontró usuario con cédula: " + solicitud.getCedulaPaciente());
                return false;
            }

            UsuarioSalud usuario = usuarioOpt.get();

            // Construir mensaje de notificación
            String titulo = "Solicitud de Acceso a Documento";
            String cuerpo = String.format(
                "%s (CI %d) de %s solicita acceso a su documento del %s",
                solicitud.getProfesionalNombre(),
                solicitud.getProfesionalCi(),
                solicitud.getNombreClinica(),
                solicitud.getFechaDocumento()
            );

            // Crear entidad notificacion
            notificacion notif = new notificacion();
            notif.setTipo("SOLICITUD_ACCESO");
            notif.setMensaje(titulo + ": " + cuerpo);
            notif.setEstado("PENDIENTE");
            notif.setUsuario(usuario);
            notif.setFecCreacion(LocalDateTime.now());

            // Guardar datos adicionales como JSON
            String datosJson = gson.toJson(solicitud);
            notif.setDatosAdicionales(datosJson);

            // Persistir notificación
            notificacionDAO.save(notif);
            LOGGER.info("Notificación guardada en BD para usuario: " + solicitud.getCedulaPaciente());

            if (!puedeRecibirNotificaciones(usuario)) {
                LOGGER.info(() -> "Usuario " + solicitud.getCedulaPaciente() +
                        " deshabilitó las notificaciones. Se registró en BD pero no se enviará push.");
                return true;
            }

            // 2. Enviar notificación push via FCM
            if (!firebaseInitializer.isReady()) {
                LOGGER.warning("Firebase not initialized; notification saved in DB but not sent via FCM.");
                return true; // Consideramos éxito porque se guardó en BD
            }

            String topic = "user-" + sanitizeTopicSegment(solicitud.getCedulaPaciente());

            Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                    .setTitle(titulo)
                    .setBody(cuerpo)
                    .build())
                // Datos adicionales para la app móvil
                .putData("tipo", "SOLICITUD_ACCESO")
                .putData("cedulaPaciente", solicitud.getCedulaPaciente())
                .putData("notificacionId", notif.getId().toString())
                .putData("documentoId", solicitud.getDocumentoId())
                .putData("profesionalCi", solicitud.getProfesionalCi().toString())
                .putData("profesionalNombre", solicitud.getProfesionalNombre())
                .putData("especialidad", solicitud.getEspecialidad() != null ? solicitud.getEspecialidad() : "")
                .putData("tenantId", solicitud.getTenantId())
                .putData("nombreClinica", solicitud.getNombreClinica())
                .putData("fechaDocumento", solicitud.getFechaDocumento())
                .putData("motivoConsulta", solicitud.getMotivoConsulta())
                .putData("diagnostico", solicitud.getDiagnostico() != null ? solicitud.getDiagnostico() : "")
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .build();

            FirebaseMessaging messaging = firebaseInitializer.getMessaging();
            String response = messaging.send(message);
            LOGGER.info(String.format(
                "Notificación FCM enviada a %s. FCM response: %s",
                solicitud.getCedulaPaciente(), response));
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                String.format("Error al procesar notificación de solicitud de acceso para %s",
                    solicitud.getCedulaPaciente()), e);
            return false;
        }
    }

    private boolean puedeRecibirNotificaciones(UsuarioSalud usuario) {
        if (usuario == null) {
            return false;
        }
        boolean activo = usuario.getActive() == null || usuario.getActive();
        boolean habilitado = usuario.getNotificacionesHabilitadas() == null || usuario.getNotificacionesHabilitadas();
        return activo && habilitado;
    }
}
