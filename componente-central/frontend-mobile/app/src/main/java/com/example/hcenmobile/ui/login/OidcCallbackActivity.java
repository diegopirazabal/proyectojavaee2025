package com.example.hcenmobile.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hcenmobile.MainActivity;
import com.example.hcenmobile.R;
import com.example.hcenmobile.service.OidcAuthService;
import com.example.hcenmobile.util.Constants;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

/**
 * Activity que recibe el callback de la autenticación OIDC
 * Esta activity se abre cuando gub.uy redirige de vuelta a la app
 */
public class OidcCallbackActivity extends AppCompatActivity {

    private static final String TAG = "OidcCallbackActivity";
    private OidcAuthService oidcAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oidc_callback);

        // Ocultar action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        oidcAuthService = new OidcAuthService(this);

        // Procesar la respuesta de autenticación
        Intent intent = getIntent();
        if (intent != null) {
            handleAuthorizationResponse(intent);
        } else {
            handleAuthError("No se recibió respuesta de autenticación");
        }
    }

    private void handleAuthorizationResponse(Intent intent) {
        Uri data = intent.getData();
        
        if (data != null) {
            Log.d(TAG, "Callback URI recibida: " + data.toString());
            
            // Extraer el JWT token de la URL de callback
            String jwtToken = data.getQueryParameter("jwt_token");
            
            if (jwtToken != null && !jwtToken.isEmpty()) {
                // Guardar el JWT token
                saveJwtToken(jwtToken);

                // Extraer información del usuario
                // Primero intentar de los parámetros explícitos
                String cedula = data.getQueryParameter("cedula");
                String nombreCompleto = data.getQueryParameter("nombre_completo");

                // Si no vienen en los parámetros, decodificar el JWT
                if (cedula == null || cedula.isEmpty()) {
                    Log.d(TAG, "Cédula no disponible en parámetros, decodificando JWT...");
                    cedula = extraerCedulaDelJWT(jwtToken);
                }
                if (nombreCompleto == null || nombreCompleto.isEmpty()) {
                    nombreCompleto = extraerNombreDelJWT(jwtToken);
                }

                // Guardar información de sesión
                guardarSesionOidc(cedula, nombreCompleto, jwtToken);

                // Mostrar mensaje de éxito
                Toast.makeText(this,
                    "Autenticación exitosa con gub.uy",
                    Toast.LENGTH_SHORT).show();

                // Ir a MainActivity
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                finish();
            } else {
                handleAuthError("No se recibió el token de autenticación");
            }
        } else {
            // Intentar usar el servicio OIDC para procesar la respuesta
            oidcAuthService.handleAuthorizationResponse(intent, new OidcAuthService.AuthorizationCallback() {
                @Override
                public void onSuccess(String jwtToken) {
                    saveJwtToken(jwtToken);

                    // Extraer cédula y nombre del JWT
                    String cedula = extraerCedulaDelJWT(jwtToken);
                    String nombreCompleto = extraerNombreDelJWT(jwtToken);

                    guardarSesionOidc(cedula, nombreCompleto, jwtToken);

                    Toast.makeText(OidcCallbackActivity.this,
                        "Autenticación exitosa con gub.uy",
                        Toast.LENGTH_SHORT).show();

                    Intent mainIntent = new Intent(OidcCallbackActivity.this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    handleAuthError(error);
                }
            });
        }
    }

    private void saveJwtToken(String jwtToken) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(Constants.PREF_JWT_TOKEN, jwtToken)
                .putBoolean(Constants.PREF_IS_OIDC_AUTH, true)
                .apply();
        
        Log.d(TAG, "JWT token guardado exitosamente");
    }

    /**
     * Extrae la cédula del payload del JWT
     */
    private String extraerCedulaDelJWT(String jwtToken) {
        try {
            // JWT tiene 3 partes separadas por puntos: header.payload.signature
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                Log.e(TAG, "JWT inválido: no tiene suficientes partes");
                return null;
            }

            // Decodificar el payload (segunda parte)
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP));
            Log.d(TAG, "JWT Payload decodificado: " + payload);

            // Parsear JSON
            JSONObject jsonPayload = new JSONObject(payload);

            // Intentar obtener cédula de diferentes campos posibles
            if (jsonPayload.has("cedula")) {
                return jsonPayload.getString("cedula");
            } else if (jsonPayload.has("ci")) {
                return jsonPayload.getString("ci");
            } else if (jsonPayload.has("sub")) {
                // sub podría contener la cédula
                return jsonPayload.getString("sub");
            }

            Log.w(TAG, "No se encontró campo de cédula en el JWT");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error al decodificar cédula del JWT", e);
            return null;
        }
    }

    /**
     * Extrae el nombre completo del payload del JWT
     */
    private String extraerNombreDelJWT(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP));
            JSONObject jsonPayload = new JSONObject(payload);

            // Intentar obtener nombre de diferentes campos posibles
            if (jsonPayload.has("nombre_completo")) {
                return jsonPayload.getString("nombre_completo");
            } else if (jsonPayload.has("name")) {
                return jsonPayload.getString("name");
            } else if (jsonPayload.has("given_name") && jsonPayload.has("family_name")) {
                return jsonPayload.getString("given_name") + " " + jsonPayload.getString("family_name");
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error al decodificar nombre del JWT", e);
            return null;
        }
    }

    private void guardarSesionOidc(String cedula, String nombreCompleto, String jwtToken) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Log.d(TAG, "=== GUARDANDO SESIÓN OIDC ===");
        Log.d(TAG, "Cédula: " + (cedula != null ? cedula : "null"));
        Log.d(TAG, "Nombre: " + (nombreCompleto != null ? nombreCompleto : "null"));

        if (cedula != null && !cedula.isEmpty()) {
            editor.putString(Constants.PREF_USER_CI, cedula);
            editor.putString(Constants.PREF_USER_ID, cedula);
        } else {
            // Si no tenemos cédula, usar un identificador genérico
            editor.putString(Constants.PREF_USER_ID, "oidc_user");
            Log.w(TAG, "⚠ Cédula no disponible, usando ID genérico");
        }

        if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
            editor.putString(Constants.PREF_USER_NAME, nombreCompleto);
        }

        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putBoolean(Constants.PREF_IS_OIDC_AUTH, true);
        editor.apply();

        Log.d(TAG, "✓ Sesión OIDC guardada exitosamente");

        // Suscribirse al topic FCM del usuario para recibir notificaciones personalizadas
        if (cedula != null && !cedula.isEmpty()) {
            suscribirseAlTopicDelUsuario(cedula);
        } else {
            Log.w(TAG, "⚠ No se pudo suscribir al topic FCM: cédula no disponible después de decodificar JWT");
        }
    }

    /**
     * Suscribe el dispositivo al topic FCM del usuario para recibir notificaciones personalizadas
     * Topic format: "user-<cedula>"
     */
    private void suscribirseAlTopicDelUsuario(String cedula) {
        String topic = "user-" + cedula;
        Log.d(TAG, "==== SUSCRIPCIÓN AL TOPIC FCM (OIDC) ====");
        Log.d(TAG, "Suscribiéndose al topic: " + topic);
        Log.d(TAG, "=========================================");

        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✓✓✓ Suscrito exitosamente al topic: " + topic + " (login OIDC) ✓✓✓");
                    } else {
                        Log.e(TAG, "❌ Error al suscribirse al topic: " + topic, task.getException());
                    }
                });
    }

    private void handleAuthError(String error) {
        Log.e(TAG, "Error en autenticación OIDC: " + error);
        
        Toast.makeText(this,
            "Error en la autenticación: " + error,
            Toast.LENGTH_LONG).show();
        
        // Volver a la pantalla de login
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (oidcAuthService != null) {
            oidcAuthService.dispose();
        }
    }
}
