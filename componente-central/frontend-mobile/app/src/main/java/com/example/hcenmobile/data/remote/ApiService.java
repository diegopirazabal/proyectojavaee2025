package com.example.hcenmobile.data.remote;

import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.AprobarSolicitudRequest;
import com.example.hcenmobile.data.remote.dto.FCMTokenDTO;
import com.example.hcenmobile.data.remote.dto.HistoriaClinicaDocumentoDTO;
import com.example.hcenmobile.data.remote.dto.NotificacionDTO;
import com.example.hcenmobile.data.remote.dto.PermisoActivoDTO;
import com.example.hcenmobile.data.remote.dto.RechazarSolicitudRequest;
import com.example.hcenmobile.data.remote.dto.SolicitudAccesoDTO;

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
     * Obtiene los documentos registrados en la historia clínica central para un usuario.
     */
    @GET("historia-clinica/{userId}/documentos")
    Call<ApiResponse<List<HistoriaClinicaDocumentoDTO>>> getHistoriaClinicaDocumentos(
            @Path("userId") String userId);

    // ============ GESTIÓN DE PERMISOS ============

    /**
     * Obtiene las solicitudes de acceso a documentos pendientes de aprobación
     */
    @GET("notifications/solicitudes-pendientes/{cedula}")
    Call<ApiResponse<List<SolicitudAccesoDTO>>> getSolicitudesPendientes(
            @Path("cedula") String cedula);

    /**
     * Aprueba una solicitud de acceso a documento clínico
     */
    @POST("notifications/aprobar-solicitud")
    Call<ApiResponse<Void>> aprobarSolicitud(@Body AprobarSolicitudRequest request);

    /**
     * Rechaza una solicitud de acceso a documento clínico
     */
    @POST("notifications/rechazar-solicitud")
    Call<ApiResponse<Void>> rechazarSolicitud(@Body RechazarSolicitudRequest request);

    /**
     * Obtiene los permisos activos otorgados por el paciente
     */
    @GET("politicas-acceso/historia/{historiaId}/activos")
    Call<ApiResponse<List<PermisoActivoDTO>>> getPermisosActivos(
            @Path("historiaId") String historiaId);

    /**
     * Revoca un permiso otorgado previamente
     */
    @PUT("politicas-acceso/{permisoId}/revocar")
    Call<ApiResponse<Void>> revocarPermiso(
            @Path("permisoId") String permisoId,
            @Body RevocarPermisoRequest request);

    // Clase interna para request de revocar permiso
    class RevocarPermisoRequest {
        private String motivo;

        public RevocarPermisoRequest(String motivo) {
            this.motivo = motivo;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
    }
}
