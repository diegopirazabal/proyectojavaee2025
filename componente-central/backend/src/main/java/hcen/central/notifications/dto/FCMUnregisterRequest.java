package hcen.central.notifications.dto;

import java.io.Serializable;

/**
 * DTO para solicitud de eliminación de token FCM
 */
public class FCMUnregisterRequest implements Serializable {

    private String token;

    public FCMUnregisterRequest() {}

    public FCMUnregisterRequest(String token) {
        this.token = token;
    }

    // Getters y Setters

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "FCMUnregisterRequest{token='" + (token != null ? "***" : "null") + "'}";
    }
}
