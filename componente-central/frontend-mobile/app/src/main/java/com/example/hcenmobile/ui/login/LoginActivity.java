package com.example.hcenmobile.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hcenmobile.MainActivity;
import com.example.hcenmobile.R;
import com.example.hcenmobile.service.OidcAuthService;
import com.example.hcenmobile.util.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Activity de login que solicita solo la cédula de identidad
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    private TextInputEditText editTextCedula;
    private TextInputLayout textInputLayoutCedula;
    private MaterialButton buttonLogin;
    private MaterialButton buttonOidcLogin;
    private OidcAuthService oidcAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ocultar action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        oidcAuthService = new OidcAuthService(this);
        
        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextCedula = findViewById(R.id.edit_text_cedula);
        textInputLayoutCedula = findViewById(R.id.text_input_layout_cedula);
        buttonLogin = findViewById(R.id.button_login);
        buttonOidcLogin = findViewById(R.id.button_oidc_login);
    }

    private void setupListeners() {
        buttonLogin.setOnClickListener(v -> intentarLogin());
        
        buttonOidcLogin.setOnClickListener(v -> iniciarAutenticacionOidc());

        // Limpiar error al escribir
        editTextCedula.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textInputLayoutCedula.setError(null);
            }
        });
    }

    private void intentarLogin() {
        String cedula = editTextCedula.getText() != null ?
                editTextCedula.getText().toString().trim() : "";

        // Validar cédula
        if (TextUtils.isEmpty(cedula)) {
            textInputLayoutCedula.setError(getString(R.string.login_error_empty));
            return;
        }

        if (!esCedulaValida(cedula)) {
            textInputLayoutCedula.setError(getString(R.string.login_error_invalid));
            return;
        }

        // Guardar cédula en SharedPreferences
        guardarSesion(cedula);

        // Mostrar mensaje de éxito
        Toast.makeText(this,
                getString(R.string.login_success) + ", C.I.: " + cedula,
                Toast.LENGTH_SHORT).show();

        // Ir a MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Cerrar LoginActivity para que no se pueda volver con el botón atrás
    }

    private boolean esCedulaValida(String cedula) {
        // Verificar que solo contenga números
        if (!cedula.matches("\\d+")) {
            return false;
        }

        // Verificar longitud (C.I. uruguayas tienen entre 6 y 8 dígitos)
        int length = cedula.length();
        return length >= Constants.MIN_CI_LENGTH && length <= Constants.MAX_CI_LENGTH;
    }

    private void guardarSesion(String cedula) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(Constants.PREF_USER_CI, cedula)
                .putString(Constants.PREF_USER_ID, cedula) // Usar C.I. como userId
                .putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                .putBoolean(Constants.PREF_IS_OIDC_AUTH, false)
                .apply();

        // Suscribirse al topic FCM del usuario para recibir notificaciones personalizadas
        suscribirseAlTopicDelUsuario(cedula);
    }

    /**
     * Suscribe el dispositivo al topic FCM del usuario para recibir notificaciones personalizadas
     * Topic format: "user-<cedula>"
     */
    private void suscribirseAlTopicDelUsuario(String cedula) {
        String topic = "user-" + cedula;
        Log.d(TAG, "==== SUSCRIPCIÓN AL TOPIC FCM ====");
        Log.d(TAG, "Suscribiéndose al topic: " + topic);
        Log.d(TAG, "==================================");

        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✓✓✓ Suscrito exitosamente al topic: " + topic + " ✓✓✓");
                    } else {
                        Log.e(TAG, "❌ Error al suscribirse al topic: " + topic, task.getException());
                    }
                });
    }
    
    /**
     * Inicia el flujo de autenticación OIDC con gub.uy
     */
    private void iniciarAutenticacionOidc() {
        try {
            // Construir la URL de login OIDC
            String oidcUrl = OidcAuthService.getOidcLoginUrl();
            
            Log.d(TAG, "==== INICIANDO LOGIN OIDC ====");
            Log.d(TAG, "URL completa: " + oidcUrl);
            Log.d(TAG, "================================");
            
            // Abrir en el navegador para iniciar el flujo OIDC
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(oidcUrl));
            startActivity(browserIntent);
            
            Toast.makeText(this, 
                "Redirigiendo a gub.uy...", 
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar autenticación OIDC", e);
            Toast.makeText(this,
                "Error al iniciar autenticación: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (oidcAuthService != null) {
            oidcAuthService.dispose();
        }
    }
}
