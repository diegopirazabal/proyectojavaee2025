package com.example.hcenmobile.util;

/**
 * Constantes globales de la aplicación HCEN Mobile
 */
public class Constants {

    // API Configuration
    public static final String BASE_URL = "https://hcen-uy.web.elasticloud.uy/api/";
    public static final int CONNECT_TIMEOUT = 30; // seconds
    public static final int READ_TIMEOUT = 30; // seconds
    public static final int WRITE_TIMEOUT = 30; // seconds

    // SharedPreferences
    public static final String PREFS_NAME = "HCENPrefs";
    public static final String PREF_FCM_TOKEN = "fcm_token";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_CI = "user_ci";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";

    // Login
    public static final int MIN_CI_LENGTH = 6;
    public static final int MAX_CI_LENGTH = 8;

    // Notification Types
    public static final String NOTIF_TYPE_ACCESS_REQUEST = "NUEVO_PEDIDO_ACCESO";
    public static final String NOTIF_TYPE_ACCESS_GRANTED = "NUEVO_ACCESO";
    public static final String NOTIF_TYPE_HISTORY_ACCESSED = "ACCESO_HISTORIA";

    // Notification Channels
    public static final String CHANNEL_ID_ACCESS = "hcen_access_notifications";
    public static final String CHANNEL_NAME_ACCESS = "Notificaciones de Acceso";
    public static final String CHANNEL_DESC_ACCESS = "Notificaciones sobre accesos a su historia clínica";

    // Room Database
    public static final String DATABASE_NAME = "hcen_db";
    public static final int DATABASE_VERSION = 1;

    // API Endpoints
    public static final String ENDPOINT_NOTIFICATIONS = "api/notifications";
    public static final String ENDPOINT_REGISTER_TOKEN = "api/fcm/register";
    public static final String ENDPOINT_USER_INFO = "api/user/info";
    public static final String ENDPOINT_HISTORIA_CLINICA = "api/historia-clinica";

    // Request codes
    public static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1001;

    // Error messages
    public static final String ERROR_NETWORK = "Error de conexión. Por favor, verifique su conexión a internet.";
    public static final String ERROR_SERVER = "Error del servidor. Intente nuevamente más tarde.";
    public static final String ERROR_UNKNOWN = "Ha ocurrido un error inesperado.";

    private Constants() {
        // Private constructor to prevent instantiation
    }
}
