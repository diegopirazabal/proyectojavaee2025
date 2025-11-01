package hcen.central.inus.security.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import hcen.central.inus.dto.OIDCUserInfo;
import hcen.central.inus.security.config.OIDCConfiguration;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para obtener información del usuario desde el endpoint UserInfo de gub.uy
 */
@Stateless
public class OIDCUserInfoService {

    private static final Logger LOGGER = Logger.getLogger(OIDCUserInfoService.class.getName());

    @Inject
    private OIDCConfiguration oidcConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtiene la información del usuario desde el endpoint UserInfo
     *
     * @param accessToken Access token recibido del proveedor OIDC
     * @return OIDCUserInfo con los datos del usuario
     * @throws Exception si ocurre un error en la llamada HTTP o parsing
     */
    public OIDCUserInfo getUserInfo(String accessToken) throws Exception {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token no puede ser nulo o vacío");
        }

        String userInfoEndpoint = oidcConfig.getUserinfoEndpoint();
        LOGGER.info("Llamando al endpoint UserInfo: " + userInfoEndpoint);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(userInfoEndpoint);
            
            // Agregar el access token en el header Authorization
            httpGet.setHeader("Authorization", "Bearer " + accessToken);
            httpGet.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    LOGGER.log(Level.SEVERE, "Error en UserInfo endpoint. Status: " + statusCode + ", Body: " + responseBody);
                    throw new RuntimeException("Error obteniendo UserInfo. Status: " + statusCode);
                }

                LOGGER.info("UserInfo obtenido exitosamente");
                LOGGER.fine("UserInfo response: " + responseBody);

                // Parsear JSON a DTO
                OIDCUserInfo userInfo = objectMapper.readValue(responseBody, OIDCUserInfo.class);

                // Validar que los campos esenciales estén presentes
                validateUserInfo(userInfo);

                return userInfo;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo UserInfo", e);
            throw new RuntimeException("Error obteniendo UserInfo: " + e.getMessage(), e);
        }
    }

    /**
     * Valida que el UserInfo contenga los campos esenciales
     */
    private void validateUserInfo(OIDCUserInfo userInfo) throws Exception {
        if (userInfo.getSub() == null || userInfo.getSub().isEmpty()) {
            throw new IllegalStateException("UserInfo no contiene el campo 'sub' (subject)");
        }

        LOGGER.info("UserInfo validado para subject: " + userInfo.getSub());
    }

    /**
     * Obtiene el subject (sub) del usuario de forma rápida
     *
     * @param accessToken Access token del usuario
     * @return El subject (identificador único) del usuario
     * @throws Exception si ocurre un error
     */
    public String getUserSubject(String accessToken) throws Exception {
        OIDCUserInfo userInfo = getUserInfo(accessToken);
        return userInfo.getSub();
    }
}
