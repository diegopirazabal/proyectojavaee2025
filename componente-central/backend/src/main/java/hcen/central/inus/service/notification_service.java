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
}
