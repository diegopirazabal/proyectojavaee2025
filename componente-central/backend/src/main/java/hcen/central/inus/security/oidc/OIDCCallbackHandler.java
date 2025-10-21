package hcen.central.inus.security.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import hcen.central.inus.dto.OIDCTokenResponse;
import hcen.central.inus.security.config.OIDCConfiguration;
import hcen.central.inus.security.pkce.PKCEValidator;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maneja el callback de gub.uy después de la autenticación
 * Procesa el authorization code y obtiene tokens usando PKCE
 */
@Stateless
public class OIDCCallbackHandler {

    private static final Logger LOGGER = Logger.getLogger(OIDCCallbackHandler.class.getName());

    @Inject
    private OIDCConfiguration oidcConfig;

    @Inject
    private PKCEValidator pkceValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Procesa el callback de autenticación OIDC
     * Intercambia el authorization code por tokens
     *
     * @param code          Authorization code recibido del proveedor
     * @param state         State parameter para validación CSRF
     * @param codeVerifier  Code verifier PKCE usado en la petición original
     * @param redirectUri   URI de redirección usada en la petición original
     * @return OIDCTokenResponse con los tokens recibidos
     * @throws Exception si ocurre un error en el intercambio
     */
    public OIDCTokenResponse handleCallback(String code, String state, String codeVerifier, String redirectUri) throws Exception {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Authorization code no puede ser nulo o vacío");
        }

        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new IllegalArgumentException("Code verifier no puede ser nulo o vacío");
        }

        LOGGER.info("Procesando callback OIDC con code: " + code.substring(0, Math.min(10, code.length())) + "...");

        // Intercambiar code por tokens
        OIDCTokenResponse tokenResponse = exchangeCodeForTokens(code, codeVerifier, redirectUri);

        LOGGER.info("Éxito al intercambiar code por tokens");
        return tokenResponse;
    }

    /**
     * Intercambia el authorization code por tokens mediante POST al token endpoint
     *
     * @param code         Authorization code
     * @param codeVerifier Code verifier PKCE
     * @param redirectUri  Redirect URI
     * @return OIDCTokenResponse con access_token, id_token, refresh_token
     * @throws Exception si falla el intercambio
     */
    private OIDCTokenResponse exchangeCodeForTokens(String code, String codeVerifier, String redirectUri) throws Exception {
        String tokenEndpoint = oidcConfig.getTokenEndpoint();
        LOGGER.info("Intercambiando code por tokens en: " + tokenEndpoint);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenEndpoint);

            // Preparar header de autenticación Basic (client_id:client_secret)
            String clientAuth = oidcConfig.getClientId() + ":" + oidcConfig.getClientSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(clientAuth.getBytes());
            httpPost.setHeader("Authorization", "Basic " + encodedAuth);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // Preparar parámetros del POST
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "authorization_code"));
            params.add(new BasicNameValuePair("code", code));
            params.add(new BasicNameValuePair("redirect_uri", redirectUri));
            params.add(new BasicNameValuePair("code_verifier", codeVerifier));
            params.add(new BasicNameValuePair("client_id", oidcConfig.getClientId()));

            httpPost.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                LOGGER.fine("Token endpoint response status: " + statusCode);
                LOGGER.fine("Token endpoint response body: " + responseBody);

                if (statusCode != 200) {
                    LOGGER.log(Level.SEVERE, "Error en token endpoint. Status: " + statusCode + ", Body: " + responseBody);
                    throw new RuntimeException("Error intercambiando code por tokens. Status: " + statusCode);
                }

                // Parsear respuesta JSON a DTO
                OIDCTokenResponse tokenResponse = objectMapper.readValue(responseBody, OIDCTokenResponse.class);

                // Validar que los tokens esenciales estén presentes
                validateTokenResponse(tokenResponse);

                return tokenResponse;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error intercambiando code por tokens", e);
            throw new RuntimeException("Error intercambiando code por tokens: " + e.getMessage(), e);
        }
    }

    /**
     * Valida que la respuesta de tokens contenga los campos necesarios
     */
    private void validateTokenResponse(OIDCTokenResponse tokenResponse) throws Exception {
        if (tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isEmpty()) {
            throw new IllegalStateException("Token response no contiene access_token");
        }

        if (tokenResponse.getIdToken() == null || tokenResponse.getIdToken().isEmpty()) {
            throw new IllegalStateException("Token response no contiene id_token");
        }

        LOGGER.info("Token response validado exitosamente");
    }

    /**
     * Maneja errores en el callback (cuando gub.uy retorna error en lugar de code)
     *
     * @param error            Código de error
     * @param errorDescription Descripción del error
     * @throws Exception siempre lanza excepción con el error recibido
     */
    public void handleError(String error, String errorDescription) throws Exception {
        String errorMessage = "Error en callback OIDC: " + error;
        if (errorDescription != null && !errorDescription.isEmpty()) {
            errorMessage += " - " + errorDescription;
        }

        LOGGER.log(Level.SEVERE, errorMessage);
        throw new RuntimeException(errorMessage);
    }

    /**
     * Valida el state parameter para prevenir CSRF
     *
     * @param receivedState State recibido en el callback
     * @param expectedState State esperado (guardado en sesión)
     * @return true si el state es válido
     */
    public boolean validateState(String receivedState, String expectedState) {
        if (receivedState == null || expectedState == null) {
            LOGGER.warning("State parameter nulo en validación");
            return false;
        }

        boolean isValid = receivedState.equals(expectedState);
        if (!isValid) {
            LOGGER.log(Level.SEVERE, "State parameter inválido. Posible ataque CSRF.");
        }

        return isValid;
    }
}
