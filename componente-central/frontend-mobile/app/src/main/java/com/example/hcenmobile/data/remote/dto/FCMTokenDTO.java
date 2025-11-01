package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para registrar el token FCM en el backend
 * Debe coincidir con FCMTokenRequest del backend
 */
public class FCMTokenDTO {

    @SerializedName("token")
    private String token;

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("deviceModel")
    private String deviceModel;

    @SerializedName("osVersion")
    private String osVersion;

    public FCMTokenDTO() {
    }

    public FCMTokenDTO(String token, String deviceId, String deviceModel, String osVersion) {
        this.token = token;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
    }

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
}
