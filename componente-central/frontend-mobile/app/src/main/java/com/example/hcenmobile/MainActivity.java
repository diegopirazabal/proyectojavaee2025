package com.example.hcenmobile;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.hcenmobile.databinding.ActivityMainBinding;
import com.example.hcenmobile.data.repository.NotificacionRepository;
import com.example.hcenmobile.util.Constants;
import com.example.hcenmobile.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    // Launcher para solicitar permisos de notificaciones (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Permiso de notificaciones concedido");
                    obtenerTokenFCM();
                } else {
                    Log.w(TAG, "Permiso de notificaciones denegado");
                    Toast.makeText(this, "Permiso de notificaciones denegado. No recibirá notificaciones push.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar si el usuario está logueado
        if (!verificarSesion()) {
            // Si no está logueado, ir a LoginActivity
            redirigirALogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Solicitar permisos de notificaciones y obtener token de Firebase
        solicitarPermisosNotificaciones();
    }

    /**
     * Verifica si el usuario está logueado
     */
    private boolean verificarSesion() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    /**
     * Redirige a LoginActivity y cierra MainActivity
     */
    private void redirigirALogin() {
        Intent intent = new Intent(this, com.example.hcenmobile.ui.login.LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Solicita permisos de notificaciones (Android 13+)
     */
    private void solicitarPermisosNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permisos de notificaciones ya concedidos");
                obtenerTokenFCM();
            } else {
                // Solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // En versiones anteriores a Android 13, los permisos son automáticos
            obtenerTokenFCM();
        }
    }

    /**
     * Obtiene el token de Firebase Cloud Messaging y lo registra
     */
    private void obtenerTokenFCM() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Error al obtener token FCM", task.getException());
                        return;
                    }

                    // Obtener token
                    String token = task.getResult();
                    Log.d(TAG, "Token FCM obtenido: " + token);

                    // Guardar token en SharedPreferences
                    guardarTokenLocal(token);

                    // Registrar token en el backend
                    registrarTokenEnBackend(token);

                    Toast.makeText(MainActivity.this, "Notificaciones activadas",
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Guarda el token FCM en SharedPreferences
     */
    private void guardarTokenLocal(String token) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(Constants.PREF_FCM_TOKEN, token)
                .apply();
        Log.d(TAG, "Token guardado localmente");
    }

    /**
     * Registra el token FCM en el backend
     */
    private void registrarTokenEnBackend(String token) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        // Usar la C.I. guardada como userId
        String userId = prefs.getString(Constants.PREF_USER_CI, "unknown");
        String deviceInfo = Build.MODEL + " (" + Build.VERSION.RELEASE + ")";

        NotificacionRepository repository = NotificacionRepository.getInstance(getApplicationContext());
        repository.registrarTokenFCM(token, userId, deviceInfo,
                new NotificacionRepository.TokenCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Token registrado exitosamente en el backend");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error al registrar token en el backend: " + error);
                // No mostramos error al usuario ya que el token está guardado localmente
                // y se puede intentar registrar más tarde
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mostrarDialogoLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Muestra un diálogo de confirmación para el logout
     */
    private void mostrarDialogoLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirmation_title)
                .setMessage(R.string.logout_confirmation_message)
                .setPositiveButton(R.string.button_logout, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        realizarLogout();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    /**
     * Realiza el logout: limpia sesión y redirige a LoginActivity
     */
    private void realizarLogout() {
        // Limpiar sesión usando SessionManager
        SessionManager.logout(this);

        // Mostrar mensaje
        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();

        // Redirigir a LoginActivity
        redirigirALogin();
    }
}