package com.example.hcenmobile.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hcenmobile.MainActivity;
import com.example.hcenmobile.R;
import com.example.hcenmobile.service.OidcAuthService;
import com.example.hcenmobile.util.Constants;

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
                
                // Extraer información del usuario si está disponible
                String cedula = data.getQueryParameter("cedula");
                String nombreCompleto = data.getQueryParameter("nombre_completo");
                
                // Guardar información de sesión
                guardarSesionOidc(cedula, nombreCompleto);
                
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
                    guardarSesionOidc(null, null);
                    
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

    private void guardarSesionOidc(String cedula, String nombreCompleto) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (cedula != null && !cedula.isEmpty()) {
            editor.putString(Constants.PREF_USER_CI, cedula);
            editor.putString(Constants.PREF_USER_ID, cedula);
        } else {
            // Si no tenemos cédula, usar un identificador genérico
            editor.putString(Constants.PREF_USER_ID, "oidc_user");
        }
        
        if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
            editor.putString(Constants.PREF_USER_NAME, nombreCompleto);
        }
        
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putBoolean(Constants.PREF_IS_OIDC_AUTH, true);
        editor.apply();
        
        Log.d(TAG, "Sesión OIDC guardada");
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
