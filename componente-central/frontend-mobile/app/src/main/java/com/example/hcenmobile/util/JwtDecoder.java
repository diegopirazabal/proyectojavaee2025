package com.example.hcenmobile.util;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Utilidad para decodificar JWT y extraer claims sin validar la firma.
 * IMPORTANTE: No valida la firma del JWT. La validación se hace en el backend.
 */
public class JwtDecoder {

    private static final String TAG = "JwtDecoder";

    /**
     * Extrae un claim específico del JWT
     *
     * @param jwt       Token JWT
     * @param claimName Nombre del claim
     * @return Valor del claim o null si no existe
     */
    public static String extractClaim(String jwt, String claimName) {
        if (jwt == null || jwt.isEmpty()) {
            return null;
        }

        try {
            // JWT tiene formato: header.payload.signature
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                Log.w(TAG, "JWT no tiene el formato correcto");
                return null;
            }

            // Decodificar el payload (segunda parte)
            String payload = parts[1];
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_WRAP);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            // Parsear JSON
            JSONObject jsonObject = new JSONObject(decodedPayload);

            if (jsonObject.has(claimName)) {
                return jsonObject.getString(claimName);
            }

            return null;

        } catch (JSONException e) {
            Log.e(TAG, "Error parseando JWT JSON", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error extrayendo claim '" + claimName + "' del JWT", e);
            return null;
        }
    }

    /**
     * Extrae el tipo de documento del JWT
     *
     * @param jwt Token JWT
     * @return Tipo de documento (DO, PA, OTRO) o null
     */
    public static String extractDocType(String jwt) {
        return extractClaim(jwt, "docType");
    }

    /**
     * Extrae el número de documento del JWT
     *
     * @param jwt Token JWT
     * @return Número de documento o null
     */
    public static String extractDocNumber(String jwt) {
        return extractClaim(jwt, "docNumber");
    }

    /**
     * Extrae el subject (userSub) del JWT
     *
     * @param jwt Token JWT
     * @return Subject o null
     */
    public static String extractSubject(String jwt) {
        return extractClaim(jwt, "sub");
    }

    /**
     * Extrae el email del JWT
     *
     * @param jwt Token JWT
     * @return Email o null
     */
    public static String extractEmail(String jwt) {
        return extractClaim(jwt, "email");
    }
}
