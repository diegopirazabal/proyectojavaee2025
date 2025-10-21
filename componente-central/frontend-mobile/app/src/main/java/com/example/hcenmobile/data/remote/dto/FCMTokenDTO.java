package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para registrar el token FCM en el backend
 */
public class FCMTokenDTO {

    @SerializedName("token")
    private String token;

    @SerializedName("userId")
    private String userId;

    @SerializedName("deviceInfo")
    private String deviceInfo;

    public FCMTokenDTO() {
    }

    public FCMTokenDTO(String token, String userId, String deviceInfo) {
        this.token = token;
        this.userId = userId;
        this.deviceInfo = deviceInfo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
