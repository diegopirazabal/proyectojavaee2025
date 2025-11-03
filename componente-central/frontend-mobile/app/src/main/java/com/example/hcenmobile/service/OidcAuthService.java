package com.example.hcenmobile.service;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

/**
 * Servicio para manejar la autenticación OIDC con gub.uy
 */
public class OidcAuthService {

    private static final String TAG = "OidcAuthService";
    
    // Backend HCEN que maneja el flujo OIDC
    private static final String BACKEND_URL = "https://hcen-uy.web.elasticloud.uy/api";
    
    // URL del backend para iniciar login OIDC
    private static final String AUTH_ENDPOINT = BACKEND_URL + "/auth/login";
    
    // URL donde el backend enviará el JWT después de la autenticación
    private static final String TOKEN_ENDPOINT = BACKEND_URL + "/auth/callback";
    
    // Esquema personalizado para recibir el callback en la app móvil
    private static final String REDIRECT_URI = "hcenmobile://auth/callback";
    
    // Client ID (si es necesario para el backend)
    private static final String CLIENT_ID = "hcen-mobile-app";
    
    private final Context context;
    private final AuthorizationService authService;

    public OidcAuthService(Context context) {
        this.context = context;
        this.authService = new AuthorizationService(context);
    }

    /**
     * Inicia el flujo de autenticación OIDC
     * SIMPLIFICADO: Solo abre el navegador con la URL del backend
     * El backend maneja todo el flujo OIDC con gub.uy
     */
    public Intent createAuthorizationIntent() {
        // Construir URL simple como lo hace la web
        String loginUrl = getOidcLoginUrl();
        
        // Crear intent para abrir navegador
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl));
        
        return browserIntent;
    }

    /**
     * Procesa la respuesta de autorización recibida en el callback
     * @param intent Intent recibido en la Activity de callback
     * @param callback Callback para manejar el resultado
     */
    public void handleAuthorizationResponse(
            Intent intent,
            AuthorizationCallback callback
    ) {
        net.openid.appauth.AuthorizationResponse response =
                net.openid.appauth.AuthorizationResponse.fromIntent(intent);
        AuthorizationException exception = AuthorizationException.fromIntent(intent);

        if (response != null) {
            Log.d(TAG, "Authorization successful: " + response.authorizationCode);
            
            // En nuestro caso, el backend ya maneja el intercambio del código
            // por el token y redirige con el JWT en la URL
            // Extraer el JWT de los parámetros adicionales si existe
            String jwtToken = extractJwtFromResponse(intent);
            
            if (jwtToken != null) {
                callback.onSuccess(jwtToken);
            } else {
                callback.onError("No se recibió el token JWT");
            }
        } else if (exception != null) {
            Log.e(TAG, "Authorization failed: " + exception.getMessage());
            callback.onError(exception.getMessage());
        } else {
            callback.onError("Respuesta de autorización desconocida");
        }
    }

    /**
     * Extrae el JWT token de la URI de respuesta
     */
    private String extractJwtFromResponse(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            // El backend puede enviar el JWT como parámetro de query
            String jwt = uri.getQueryParameter("jwt_token");
            if (jwt != null && !jwt.isEmpty()) {
                return jwt;
            }
            
            // O como fragmento
            String fragment = uri.getFragment();
            if (fragment != null && fragment.contains("jwt_token=")) {
                String[] parts = fragment.split("jwt_token=");
                if (parts.length > 1) {
                    return parts[1].split("&")[0];
                }
            }
        }
        return null;
    }

    /**
     * Limpia los recursos del servicio de autorización
     */
    public void dispose() {
        authService.dispose();
    }

    /**
     * Callback para manejar el resultado de la autenticación
     */
    public interface AuthorizationCallback {
        void onSuccess(String jwtToken);
        void onError(String error);
    }

    /**
     * Obtiene la URL de login OIDC del backend
     * Esta URL redirige al usuario a gub.uy para autenticarse
     */
    public static String getOidcLoginUrl() {
        return AUTH_ENDPOINT + "?redirect_uri=" + 
               Uri.encode(REDIRECT_URI) + 
               "&origin=mobile";
    }
}
