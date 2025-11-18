package com.example.hcenmobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta genérica de la API REST
 */
public class ApiResponse<T> {

    @SerializedName("success")
    private Boolean success;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("error")
    private String error;

    public ApiResponse() {
    }

    public ApiResponse(Boolean success, String message, T data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        if (success != null) {
            return success;
        }
        if (status != null) {
            return "OK".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
        }
        return false;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
