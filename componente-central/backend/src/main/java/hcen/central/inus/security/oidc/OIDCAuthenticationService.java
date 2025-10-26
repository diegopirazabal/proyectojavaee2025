package hcen.central.inus.security.oidc;

import hcen.central.inus.dao.OIDCUserDAO;
import hcen.central.inus.dto.JWTTokenResponse;
import hcen.central.inus.dto.OIDCAuthRequest;
import hcen.central.inus.dto.OIDCTokenResponse;
import hcen.central.inus.dto.OIDCUserInfo;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.security.config.OIDCConfiguration;
import hcen.central.inus.security.jwt.JWTTokenProvider;
// import hcen.central.inus.security.pkce.PKCEGenerator; // PKCE removido
import io.jsonwebtoken.Claims;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio principal de autenticación OpenID Connect
 * Orquesta el flujo completo de autenticación con gub.uy
 */
@Stateless
public class OIDCAuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OIDCAuthenticationService.class.getName());

    @Inject
    private OIDCConfiguration oidcConfig;

    // @Inject
    // private PKCEGenerator pkceGenerator; // PKCE removido

    @Inject
    private OIDCCallbackHandler callbackHandler;

    @Inject
    private OIDCTokenValidator tokenValidator;

    @Inject
    private OIDCUserInfoService userInfoService;

    @Inject
    private JWTTokenProvider jwtTokenProvider;

    @Inject
    private OIDCUserDAO userDAO;

    /**
     * Inicia el flujo de autenticación OIDC (sin PKCE)
     *
     * @param redirectUri URI de redirección configurada en tu aplicación
     * @return OIDCAuthRequest con URL, state y nonce
     */
    public OIDCAuthRequest initiateLogin(String redirectUri) {
        LOGGER.info("Iniciando flujo de login OIDC (sin PKCE)");

        // Generar state y nonce para seguridad
        String state = generateRandomString(32);
        String nonce = generateRandomString(32);

        // Construir URL de autorización (sin PKCE)
        String authorizationUrl = buildAuthorizationUrl(redirectUri, state, nonce);

        OIDCAuthRequest authRequest = new OIDCAuthRequest();
        authRequest.setAuthorizationUrl(authorizationUrl);
        authRequest.setState(state);
        authRequest.setNonce(nonce);
        // authRequest.setCodeVerifier(codeVerifier); // PKCE removido
        // authRequest.setCodeChallenge(codeChallenge); // PKCE removido

        LOGGER.info("URL de autorización generada exitosamente");
        return authRequest;
    }

    /**
     * Procesa el callback de gub.uy y completa la autenticación (sin PKCE)
     * Intercambia code por tokens, valida ID token, obtiene UserInfo,
     * crea/actualiza usuario y genera JWT propios
     *
     * @param code          Authorization code del callback
     * @param state         State parameter del callback
     * @param expectedState State esperado (guardado en sesión)
     * @param expectedNonce Nonce esperado (guardado en sesión)
     * @param redirectUri   Redirect URI usado en el inicio
     * @return JWTTokenResponse con los JWT propios de la aplicación
     * @throws Exception si falla algún paso del proceso
     */
    public JWTTokenResponse handleCallback(String code, String state, String expectedState,
                                            String expectedNonce,
                                            String redirectUri) throws Exception {
        LOGGER.info("Procesando callback de autenticación OIDC (sin PKCE)");

        // 1. Validar state (protección CSRF)
        if (!callbackHandler.validateState(state, expectedState)) {
            throw new SecurityException("State inválido. Posible ataque CSRF.");
        }

        // 2. Intercambiar code por tokens (sin PKCE)
        OIDCTokenResponse tokenResponse = callbackHandler.handleCallback(code, state, null, redirectUri);

        // 3. Validar ID Token
        Claims idTokenClaims = tokenValidator.validateIdToken(tokenResponse.getIdToken(), expectedNonce);
        String userSub = idTokenClaims.getSubject();

        LOGGER.info("ID Token validado para usuario: " + userSub);

        // 4. Obtener información del usuario desde UserInfo endpoint
        OIDCUserInfo userInfo = userInfoService.getUserInfo(tokenResponse.getAccessToken());

        // 5. Crear o actualizar usuario en la base de datos
        UsuarioSalud user = createOrUpdateUser(userInfo, tokenResponse);

        // 6. Generar JWT propios de la aplicación (stateless)
        JWTTokenResponse jwtResponse = generateApplicationJWT(user);

        LOGGER.info("Éxito en autenticación completa para usuario: " + userSub);
        return jwtResponse;
    }

    /**
     * Crea o actualiza un usuario en la base de datos
     * Mapea claims de gub.uy a UsuarioSalud:
     * - numero_documento -> cedula (PK)
     * - tipo_documento -> tipoDeDocumento
     * - nombre_completo -> nombreCompleto
     * - primer_nombre -> primerNombre
     * - segundo_nombre -> segundoNombre
     * - primer_apellido -> primerApellido
     * - segundo_apellido -> segundoApellido
     * - email -> email
     * - email_verified -> emailVerificado
     * 
     * Ignoramos: sub, uid, rid, nid, pais_documento, name, given_name, family_name, ae
     */
    private UsuarioSalud createOrUpdateUser(OIDCUserInfo userInfo, OIDCTokenResponse tokenResponse) {
        // numero_documento -> cedula
        String cedula = userInfo.getNumeroDocumento();
        
        if (cedula == null || cedula.isEmpty()) {
            throw new IllegalArgumentException("numero_documento es requerido de gub.uy");
        }

        // Buscar usuario existente por cédula
        UsuarioSalud user = userDAO.findByCedula(cedula);

        if (user == null) {
            // Crear nuevo usuario
            LOGGER.info("Creando nuevo usuario con cédula: " + cedula);
            user = new UsuarioSalud();
            user.setCedula(cedula);
            user.setCreatedAt(Instant.now());
        } else {
            LOGGER.info("Actualizando usuario existente con cédula: " + cedula);
        }

        // Mapear claims de gub.uy a UsuarioSalud
        user.setEmail(userInfo.getEmail());
        user.setEmailVerificado(userInfo.getEmailVerified() != null ? userInfo.getEmailVerified() : false);
        
        // tipo_documento -> tipoDeDocumento: mapear String de gub.uy a enum
        user.setTipoDeDocumento(mapTipoDocumento(userInfo.getTipoDocumento()));
        
        user.setNombreCompleto(userInfo.getNombreCompleto());
        user.setPrimerNombre(userInfo.getPrimerNombre());
        user.setSegundoNombre(userInfo.getSegundoNombre());
        user.setPrimerApellido(userInfo.getPrimerApellido());
        user.setSegundoApellido(userInfo.getSegundoApellido());
        
        user.setLastLogin(Instant.now());
        user.setActive(true);
        user.setUpdatedAt(Instant.now());

        // Guardar en base de datos
        user = userDAO.save(user);
        LOGGER.info("Usuario guardado con ID: " + user.getId());

        return user;
    }


    /**
     * Genera JWT propios de la aplicación para el usuario autenticado (stateless)
     */
    private JWTTokenResponse generateApplicationJWT(UsuarioSalud user) {
        LOGGER.info("Generando JWT propio de la aplicación para usuario con cédula: " + user.getCedula());

        // Roles del usuario (por ahora, rol básico "USER")
        List<String> roles = Arrays.asList("USER");

        // Generar access token JWT usando la cédula como subject
        String accessToken = jwtTokenProvider.generateAccessToken(user.getCedula(), roles);

        // Generar refresh token JWT
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getCedula());

        JWTTokenResponse jwtResponse = new JWTTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600, // expires_in en segundos
                user.getCedula(), // Usar cédula como userSub
                roles
        );

        LOGGER.info("JWT generados exitosamente");
        return jwtResponse;
    }

    /**
     * Refresca un JWT usando el refresh token
     *
     * @param refreshToken Refresh token JWT propio de la aplicación
     * @return Nuevo JWTTokenResponse
     * @throws Exception si el refresh token es inválido
     */
    public JWTTokenResponse refreshToken(String refreshToken) throws Exception {
        LOGGER.info("Refrescando JWT");

        // Validar refresh token
        Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);
        String cedula = claims.getSubject();

        // Buscar usuario por cédula
        UsuarioSalud user = userDAO.findByCedula(cedula);
        if (user == null || !user.isActive()) {
            throw new SecurityException("Usuario no encontrado o inactivo");
        }

        // Generar nuevos tokens
        List<String> roles = Arrays.asList("USER");
        String newAccessToken = jwtTokenProvider.generateAccessToken(cedula, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(cedula);

        JWTTokenResponse jwtResponse = new JWTTokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                3600,
                cedula,
                roles
        );

        LOGGER.info("JWT refrescado exitosamente para usuario con cédula: " + cedula);
        return jwtResponse;
    }

    /**
     * Logout (con JWT stateless, solo informativo)
     * El cliente debe descartar el JWT
     *
     * @param cedula Cédula del usuario
     */
    public void logout(String cedula) {
        LOGGER.info("Logout solicitado para usuario con cédula: " + cedula);
        // Con JWT stateless, el cliente simplemente descarta el token
        // No hay sesiones del lado del servidor para invalidar
    }

    /**
     * Construye la URL de autorización para redirigir al usuario (sin PKCE)
     */
    private String buildAuthorizationUrl(String redirectUri, String state, String nonce) {
        try {
            StringBuilder url = new StringBuilder(oidcConfig.getAuthorizationEndpoint());
            url.append("?response_type=code");
            url.append("&client_id=").append(oidcConfig.getClientId());
            url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
            url.append("&scope=").append(URLEncoder.encode(oidcConfig.getScope(), StandardCharsets.UTF_8));
            url.append("&state=").append(state);
            url.append("&nonce=").append(nonce);
            // PKCE removido: no se envían code_challenge ni code_challenge_method

            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error construyendo URL de autorización", e);
        }
    }

    /**
     * Mapea el tipo de documento de gub.uy (String) al enum TipoDocumento
     * <p>
     * gub.uy devuelve valores como: "CI", "Pasaporte", "DNI", etc.
     *
     * @param tipoDocGubUy String del tipo de documento desde gub.uy
     * @return TipoDocumento enum correspondiente
     */
    private TipoDocumento mapTipoDocumento(String tipoDocGubUy) {
        if (tipoDocGubUy == null || tipoDocGubUy.isEmpty()) {
            LOGGER.warning("tipo_documento vacío desde gub.uy, usando DO por defecto");
            return hcen.central.inus.enums.TipoDocumento.DO;
        }

        // Normalizar: convertir a mayúsculas y quitar espacios
        String tipoNormalizado = tipoDocGubUy.trim().toUpperCase();

        try {
            // Mapeo directo si coincide exactamente
            switch (tipoNormalizado) {
                case "CI":
                case "DO":
                case "CEDULA":
                case "CÉDULA":
                case "CEDULA DE IDENTIDAD":
                case "DOCUMENTO":
                    return hcen.central.inus.enums.TipoDocumento.DO;

                case "PA":
                case "PASAPORTE":
                case "PASSPORT":
                    return hcen.central.inus.enums.TipoDocumento.PA;

                default:
                    LOGGER.warning("Tipo de documento desconocido desde gub.uy: '" + tipoDocGubUy + "', usando OTRO");
                    return hcen.central.inus.enums.TipoDocumento.OTRO;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error mapeando tipo de documento: " + tipoDocGubUy, e);
            return hcen.central.inus.enums.TipoDocumento.DO;
        }
    }
    
    /**
     * Genera un string random para state y nonce
     */
    private String generateRandomString(int length) {
        byte[] randomBytes = new byte[length];
        new Random().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
