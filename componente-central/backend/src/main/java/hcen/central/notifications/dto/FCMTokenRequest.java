package hcen.central.notifications.dto;

import java.io.Serializable;

/**
 * DTO para solicitud de registro de token FCM
 * Corresponde al schema FCMTokenRequest del OpenAPI
 */
public class FCMTokenRequest implements Serializable {

    private String token;
    private String deviceId;
    private String deviceModel;
    private String osVersion;

    public FCMTokenRequest() {}

    public FCMTokenRequest(String token, String deviceId) {
        this.token = token;
        this.deviceId = deviceId;
    }

    // Getters y Setters

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    @Override
    public String toString() {
        return "FCMTokenRequest{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", osVersion='" + osVersion + '\'' +
                '}';
    }
}
