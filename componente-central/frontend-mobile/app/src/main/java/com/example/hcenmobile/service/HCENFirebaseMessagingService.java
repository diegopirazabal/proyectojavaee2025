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
import com.example.hcenmobile.data.remote.dto.SolicitudAccesoDTO;
import com.example.hcenmobile.data.repository.NotificacionRepository;
import com.example.hcenmobile.util.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Date;
import java.util.Map;

/**
 * Servicio de Firebase Cloud Messaging para recibir notificaciones push
 */
public class HCENFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "HCENFCMService";
    private final Gson gson = new Gson();

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
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Datos del mensaje: " + remoteMessage.getData());
            handleDataMessage(remoteMessage);
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
    private void handleDataMessage(RemoteMessage remoteMessage) {
        try {
            Map<String, String> data = remoteMessage.getData();
            RemoteMessage.Notification notificationPayload = remoteMessage.getNotification();

            // Extraer datos de la notificación
            String notifId = firstNonEmpty(data.get("notificacionId"), data.get("notificationId"));
            String tipo = firstNonEmpty(data.get("tipo"), Constants.NOTIF_TYPE_ACCESS_GRANTED);

            String titulo = firstNonEmpty(
                    data.get("titulo"),
                    notificationPayload != null ? notificationPayload.getTitle() : null,
                    buildTituloFallback(tipo)
            );

            String mensaje = firstNonEmpty(
                    data.get("mensaje"),
                    notificationPayload != null ? notificationPayload.getBody() : null,
                    buildMensajeFallback(tipo, data)
            );

            String remitente = firstNonEmpty(
                    data.get("remitente"),
                    data.get("nombreClinica"),
                    data.get("profesionalNombre")
            );

            String datosAdicionales = buildDatosAdicionales(
                    data,
                    tipo,
                    notifId,
                    mensaje
            );

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

    private String buildDatosAdicionales(Map<String, String> data, String tipo, String notifId, String mensaje) {
        String datosAdicionales = data.get("datosAdicionales");
        if (!isNullOrEmpty(datosAdicionales)) {
            return datosAdicionales;
        }

        if (isAccessRequestType(tipo)) {
            try {
                SolicitudAccesoDTO solicitud = new SolicitudAccesoDTO();
                solicitud.setId(notifId);
                solicitud.setTipo(tipo);
                solicitud.setMensaje(mensaje);
                solicitud.setEstado("PENDIENTE");
                solicitud.setFechaCreacion(data.get("timestamp"));
                solicitud.setDocumentoId(data.get("documentoId"));
                solicitud.setCedulaPaciente(data.get("cedulaPaciente"));
                solicitud.setProfesionalNombre(data.get("profesionalNombre"));
                solicitud.setEspecialidad(data.get("especialidad"));
                solicitud.setTenantId(data.get("tenantId"));
                solicitud.setNombreClinica(data.get("nombreClinica"));
                solicitud.setFechaDocumento(data.get("fechaDocumento"));
                solicitud.setMotivoConsulta(data.get("motivoConsulta"));
                solicitud.setDiagnostico(data.get("diagnostico"));

                String profesionalCi = data.get("profesionalCi");
                if (!isNullOrEmpty(profesionalCi)) {
                    try {
                        solicitud.setProfesionalCi(Integer.parseInt(profesionalCi));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "No se pudo parsear profesionalCi: " + profesionalCi, e);
                    }
                }

                return gson.toJson(solicitud);
            } catch (Exception e) {
                Log.e(TAG, "Error al construir datos adicionales para solicitud de acceso", e);
            }
        }

        return datosAdicionales;
    }

    private boolean isAccessRequestType(String tipo) {
        return "SOLICITUD_ACCESO".equalsIgnoreCase(tipo)
                || Constants.NOTIF_TYPE_ACCESS_REQUEST.equalsIgnoreCase(tipo);
    }

    private String buildTituloFallback(String tipo) {
        if (isAccessRequestType(tipo)) {
            return "Solicitud de acceso a documento";
        } else if (Constants.NOTIF_TYPE_HISTORY_ACCESSED.equalsIgnoreCase(tipo)) {
            return "Se accedió a su historia clínica";
        } else if (Constants.NOTIF_TYPE_ACCESS_GRANTED.equalsIgnoreCase(tipo)) {
            return "Nuevo acceso registrado";
        }
        return "Nueva notificación HCEN";
    }

    private String buildMensajeFallback(String tipo, Map<String, String> data) {
        if (isAccessRequestType(tipo)) {
            String profesional = firstNonEmpty(data.get("profesionalNombre"), "Un profesional de salud");
            String ci = data.get("profesionalCi");
            String clinica = firstNonEmpty(data.get("nombreClinica"), "una institución");
            String fechaDocumento = firstNonEmpty(data.get("fechaDocumento"), "su historia clínica");

            StringBuilder builder = new StringBuilder();
            builder.append(profesional);
            if (!isNullOrEmpty(ci)) {
                builder.append(" (CI ").append(ci).append(")");
            }
            builder.append(" de ").append(clinica).append(" solicita acceso a ");
            builder.append(fechaDocumento);

            return builder.toString();
        }
        return "";
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isNullOrEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
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
        // Obtener información del dispositivo
        String deviceId = android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );
        String deviceModel = Build.MODEL;
        String osVersion = "Android " + Build.VERSION.RELEASE;

        NotificacionRepository repository = NotificacionRepository.getInstance(getApplicationContext());
        repository.registrarTokenFCM(token, deviceId, deviceModel, osVersion, new NotificacionRepository.TokenCallback() {
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
