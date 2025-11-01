package com.example.hcenmobile.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestor de sesión para manejar login/logout del usuario
 */
public class SessionManager {

    /**
     * Obtiene el userId (C.I.) del usuario logueado
     */
    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_USER_CI, null);
    }

    /**
     * Obtiene la C.I. del usuario logueado
     */
    public static String getUserCI(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_USER_CI, "");
    }

    /**
     * Verifica si hay un usuario logueado
     */
    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    /**
     * Cierra la sesión del usuario (logout)
     * Elimina todos los datos de sesión de SharedPreferences
     */
    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(Constants.PREF_USER_CI)
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_IS_LOGGED_IN)
                .remove(Constants.PREF_FCM_TOKEN)
                .apply();
    }
}
