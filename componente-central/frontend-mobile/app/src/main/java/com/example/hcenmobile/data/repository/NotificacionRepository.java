package com.example.hcenmobile.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.hcenmobile.data.local.AppDatabase;
import com.example.hcenmobile.data.local.NotificacionDao;
import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.data.remote.ApiService;
import com.example.hcenmobile.data.remote.RetrofitClient;
import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.FCMTokenDTO;
import com.example.hcenmobile.data.remote.dto.NotificacionDTO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository que maneja la lógica de datos para notificaciones
 * Combina datos locales (Room) y remotos (API REST)
 */
public class NotificacionRepository {

    private static final String TAG = "NotificacionRepository";
    private static NotificacionRepository instance;

    private final NotificacionDao notificacionDao;
    private final ApiService apiService;
    private final ExecutorService executorService;
    private final SimpleDateFormat dateFormat;

    private NotificacionRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        notificacionDao = database.notificacionDao();
        apiService = RetrofitClient.getInstance().getApiService();
        executorService = Executors.newSingleThreadExecutor();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    }

    public static synchronized NotificacionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NotificacionRepository(context.getApplicationContext());
        }
        return instance;
    }

    // Métodos locales (Room)

    public LiveData<List<Notificacion>> getAllNotificaciones() {
        return notificacionDao.getAllNotificaciones();
    }

    public LiveData<List<Notificacion>> getNotificacionesNoLeidas() {
        return notificacionDao.getNotificacionesNoLeidas();
    }

    public LiveData<Integer> getCountNotificacionesNoLeidas() {
        return notificacionDao.getCountNotificacionesNoLeidas();
    }

    public void insertNotificacion(Notificacion notificacion) {
        executorService.execute(() -> {
            notificacionDao.insert(notificacion);
        });
    }

    public void marcarComoLeida(long id) {
        executorService.execute(() -> {
            notificacionDao.marcarComoLeida(id);
        });
    }

    public void marcarTodasComoLeidas() {
        executorService.execute(() -> {
            notificacionDao.marcarTodasComoLeidas();
        });
    }

    // Métodos remotos (API)

    public void sincronizarNotificaciones(String userId, SyncCallback callback) {
        apiService.getNotificaciones(userId).enqueue(new Callback<ApiResponse<List<NotificacionDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<NotificacionDTO>>> call,
                                   Response<ApiResponse<List<NotificacionDTO>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<NotificacionDTO>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // Convertir DTOs a entidades y guardar en BD local
                        executorService.execute(() -> {
                            List<Notificacion> notificaciones = convertDTOsToEntities(apiResponse.getData());
                            notificacionDao.insertAll(notificaciones);
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        });
                    } else {
                        if (callback != null) {
                            callback.onError(apiResponse.getError());
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Error de servidor: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<NotificacionDTO>>> call, Throwable t) {
                Log.e(TAG, "Error al sincronizar notificaciones", t);
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    public void registrarTokenFCM(String token, String userId, String deviceInfo, TokenCallback callback) {
        FCMTokenDTO tokenDTO = new FCMTokenDTO(token, userId, deviceInfo);

        apiService.registerFCMToken(tokenDTO).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "Token FCM registrado exitosamente");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        Log.e(TAG, "Error al registrar token: " + apiResponse.getError());
                        if (callback != null) {
                            callback.onError(apiResponse.getError());
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Error de servidor: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Error al registrar token FCM", t);
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    // Métodos auxiliares

    private List<Notificacion> convertDTOsToEntities(List<NotificacionDTO> dtos) {
        List<Notificacion> notificaciones = new ArrayList<>();
        for (NotificacionDTO dto : dtos) {
            Notificacion notificacion = new Notificacion();
            notificacion.setNotificacionId(dto.getId());
            notificacion.setTipo(dto.getTipo());
            notificacion.setTitulo(dto.getTitulo());
            notificacion.setMensaje(dto.getMensaje());
            notificacion.setRemitente(dto.getRemitente());
            notificacion.setLeida(dto.isLeida());
            notificacion.setDatosAdicionales(dto.getDatosAdicionales());

            // Parsear fecha
            try {
                Date fecha = dateFormat.parse(dto.getFechaHora());
                notificacion.setFechaHora(fecha);
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha: " + dto.getFechaHora(), e);
                notificacion.setFechaHora(new Date());
            }

            notificaciones.add(notificacion);
        }
        return notificaciones;
    }

    // Callbacks

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface TokenCallback {
        void onSuccess();
        void onError(String error);
    }
}
