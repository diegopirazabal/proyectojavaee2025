package com.example.hcenmobile.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hcenmobile.util.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor que agrega automáticamente el JWT en el header Authorization
 * de todas las peticiones HTTP al backend.
 */
public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Obtener JWT del SessionManager
        String jwtToken = SessionManager.getJwtToken(context);

        // Si no hay token, enviar request sin modificar
        if (jwtToken == null || jwtToken.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        // Agregar header Authorization con el JWT
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + jwtToken)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
