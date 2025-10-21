package com.example.hcenmobile.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.hcenmobile.MainActivity;
import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.data.repository.NotificacionRepository;
import com.example.hcenmobile.util.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.Map;

/**
 * Servicio de Firebase Cloud Messaging para recibir notificaciones push
 */
public class HCENFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "HCENFCMService";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * Se ejecuta cuando llega una nueva notificación push
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Notificación recibida de: " + remoteMessage.getFrom());

        // Procesar datos de la notificación
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Datos del mensaje: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Procesar notificación visual
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notificación: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body, remoteMessage.getData());
        }
    }

    /**
     * Se ejecuta cuando se recibe un nuevo token FCM o cuando se actualiza
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM: " + token);

        // Guardar token localmente
        saveTokenToPreferences(token);

        // Registrar token en el backend
        registerTokenWithBackend(token);
    }

    /**
     * Procesa los datos de la notificación y los guarda en la BD local
     */
    private void handleDataMessage(Map<String, String> data) {
        try {
            // Extraer datos de la notificación
            String notifId = data.get("notificationId");
            String tipo = data.get("tipo");
            String titulo = data.get("titulo");
            String mensaje = data.get("mensaje");
            String remitente = data.get("remitente");
            String datosAdicionales = data.get("datosAdicionales");

            // Crear objeto Notificacion y guardarlo en BD
            Notificacion notificacion = new Notificacion();
            notificacion.setNotificacionId(notifId);
            notificacion.setTipo(tipo != null ? tipo : Constants.NOTIF_TYPE_ACCESS_GRANTED);
            notificacion.setTitulo(titulo != null ? titulo : "Nueva notificación HCEN");
            notificacion.setMensaje(mensaje != null ? mensaje : "");
            notificacion.setRemitente(remitente);
            notificacion.setFechaHora(new Date());
            notificacion.setLeida(false);
            notificacion.setDatosAdicionales(datosAdicionales);

            // Guardar en base de datos local
            NotificacionRepository repository = NotificacionRepository.getInstance(getApplicationContext());
            repository.insertNotificacion(notificacion);

            Log.d(TAG, "Notificación guardada en BD local: " + titulo);

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar notificación", e);
        }
    }

    /**
     * Muestra una notificación en la barra de notificaciones del dispositivo
     */
    private void showNotification(String title, String message, Map<String, String> data) {
        // Intent para abrir la app cuando se toca la notificación
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Agregar datos extras al intent si es necesario
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construir la notificación
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID_ACCESS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "HCEN")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        // Mostrar la notificación
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID_ACCESS,
                    Constants.CHANNEL_NAME_ACCESS,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(Constants.CHANNEL_DESC_ACCESS);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Guarda el token FCM en SharedPreferences
     */
    private void saveTokenToPreferences(String token) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREF_FCM_TOKEN, token).apply();
        Log.d(TAG, "Token guardado en SharedPreferences");
    }

    /**
     * Registra el token en el backend
     */
    private void registerTokenWithBackend(String token) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.PREF_USER_ID, "unknown");
        String deviceInfo = Build.MODEL + " (" + Build.VERSION.RELEASE + ")";

        NotificacionRepository repository = NotificacionRepository.getInstance(getApplicationContext());
        repository.registrarTokenFCM(token, userId, deviceInfo, new NotificacionRepository.TokenCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Token registrado exitosamente en el backend");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error al registrar token en el backend: " + error);
            }
        });
    }
}
