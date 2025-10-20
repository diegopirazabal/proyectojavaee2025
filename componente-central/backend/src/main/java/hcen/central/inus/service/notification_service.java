package hcen.central.inus.service;

import jakarta.ejb.Stateless;

import java.util.logging.Logger;

@Stateless
public class notification_service {

    private static final Logger LOGGER = Logger.getLogger(notification_service.class.getName());

    public void sendBroadcastTestNotification() {
        // Placeholder implementation until Firebase integration is completed.
        LOGGER.info("Firebase broadcast test notification triggered for all users.");
    }
}
