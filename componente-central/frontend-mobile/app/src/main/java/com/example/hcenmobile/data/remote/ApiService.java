package com.example.hcenmobile.data.remote;

import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.FCMTokenDTO;
import com.example.hcenmobile.data.remote.dto.NotificacionDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interfaz de servicios REST para comunicación con el backend HCEN
 */
public interface ApiService {

    /**
     * Registra el token FCM del dispositivo en el backend
     */
    @POST("fcm/register")
    Call<ApiResponse<Void>> registerFCMToken(@Body FCMTokenDTO tokenDTO);

    /**
     * Obtiene todas las notificaciones del usuario
     */
    @GET("notifications")
    Call<ApiResponse<List<NotificacionDTO>>> getNotificaciones(@Query("userId") String userId);

    /**
     * Obtiene notificaciones no leídas
     */
    @GET("notifications/unread")
    Call<ApiResponse<List<NotificacionDTO>>> getNotificacionesNoLeidas(@Query("userId") String userId);

    /**
     * Marca una notificación como leída
     */
    @PUT("notifications/{id}/read")
    Call<ApiResponse<Void>> marcarNotificacionLeida(@Path("id") String notificacionId);

    /**
     * Marca todas las notificaciones como leídas
     */
    @PUT("notifications/read-all")
    Call<ApiResponse<Void>> marcarTodasLeidas(@Query("userId") String userId);

    // Endpoints para futuras funcionalidades

    /**
     * Obtiene información del usuario (para futuro login gub.uy)
     */
    @GET("user/info")
    Call<ApiResponse<Object>> getUserInfo(@Query("userId") String userId);

    /**
     * Obtiene la historia clínica del usuario (para futuro)
     */
    @GET("historia-clinica/{userId}")
    Call<ApiResponse<Object>> getHistoriaClinica(@Path("userId") String userId);
}
