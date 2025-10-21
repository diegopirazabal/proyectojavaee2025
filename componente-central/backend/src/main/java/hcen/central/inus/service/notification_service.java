package hcen.central.inus.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import hcen.central.inus.config.FirebaseInitializer;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class notification_service {

    private static final Logger LOGGER = Logger.getLogger(notification_service.class.getName());
    private static final String DEFAULT_TOPIC = "all-users";
    private static final String USER_TOPIC_PREFIX = "user-";

    @Inject
    private FirebaseInitializer firebaseInitializer;

    public void sendBroadcastTestNotification() {
        if (!firebaseInitializer.isReady()) {
            LOGGER.warning("Firebase not initialized; skipping broadcast test notification.");
            return;
        }

        Message message = Message.builder()
                .setTopic(DEFAULT_TOPIC)
                .setNotification(Notification.builder()
                        .setTitle("HCEN")
                        .setBody("Notificación de prueba desde el backend.")
                        .build())
                .build();

        try {
            FirebaseMessaging messaging = firebaseInitializer.getMessaging();
            String messageId = messaging.send(message);
            LOGGER.info(() -> "Firebase broadcast test notification sent. Message ID: " + messageId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send Firebase broadcast test notification.", e);
        }
    }

    public boolean sendDirectNotificationToUser(String cedula, String body) {
        if (!firebaseInitializer.isReady()) {
            LOGGER.warning("Firebase not initialized; skipping direct notification.");
            return false;
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
}
