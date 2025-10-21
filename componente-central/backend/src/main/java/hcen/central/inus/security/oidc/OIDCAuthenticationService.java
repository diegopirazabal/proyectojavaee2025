package hcen.central.inus.security.oidc;

import hcen.central.inus.dao.OIDCSessionDAO;
import hcen.central.inus.dao.OIDCUserDAO;
import hcen.central.inus.dto.JWTTokenResponse;
import hcen.central.inus.dto.OIDCAuthRequest;
import hcen.central.inus.dto.OIDCTokenResponse;
import hcen.central.inus.dto.OIDCUserInfo;
import hcen.central.inus.entity.OIDCSession;
import hcen.central.inus.entity.OIDCUser;
import hcen.central.inus.security.config.OIDCConfiguration;
import hcen.central.inus.security.jwt.JWTTokenProvider;
import hcen.central.inus.security.pkce.PKCEGenerator;
import io.jsonwebtoken.Claims;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

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

    @Inject
    private PKCEGenerator pkceGenerator;

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

    @Inject
    private OIDCSessionDAO sessionDAO;

    /**
     * Inicia el flujo de autenticación OIDC
     * Genera la URL de autorización con PKCE
     *
     * @param redirectUri URI de redirección configurada en tu aplicación
     * @return OIDCAuthRequest con URL, state, nonce, code_verifier y code_challenge
     */
    public OIDCAuthRequest initiateLogin(String redirectUri) {
        LOGGER.info("Iniciando flujo de login OIDC");

        // Generar PKCE
        String codeVerifier = pkceGenerator.generateCodeVerifier();
        String codeChallenge = pkceGenerator.generateCodeChallenge(codeVerifier);

        // Generar state y nonce para seguridad
        String state = generateRandomString(32);
        String nonce = generateRandomString(32);

        // Construir URL de autorización
        String authorizationUrl = buildAuthorizationUrl(redirectUri, codeChallenge, state, nonce);

        OIDCAuthRequest authRequest = new OIDCAuthRequest();
        authRequest.setAuthorizationUrl(authorizationUrl);
        authRequest.setState(state);
        authRequest.setNonce(nonce);
        authRequest.setCodeVerifier(codeVerifier);
        authRequest.setCodeChallenge(codeChallenge);

        LOGGER.info("URL de autorización generada exitosamente");
        return authRequest;
    }

    /**
     * Procesa el callback de gub.uy y completa la autenticación
     * Intercambia code por tokens, valida ID token, obtiene UserInfo,
     * crea/actualiza usuario y genera JWT propios
     *
     * @param code          Authorization code del callback
     * @param state         State parameter del callback
     * @param expectedState State esperado (guardado en sesión)
     * @param expectedNonce Nonce esperado (guardado en sesión)
     * @param codeVerifier  Code verifier PKCE (guardado en sesión)
     * @param redirectUri   Redirect URI usado en el inicio
     * @return JWTTokenResponse con los JWT propios de la aplicación
     * @throws Exception si falla algún paso del proceso
     */
    public JWTTokenResponse handleCallback(String code, String state, String expectedState,
                                            String expectedNonce, String codeVerifier,
                                            String redirectUri) throws Exception {
        LOGGER.info("Procesando callback de autenticación OIDC");

        // 1. Validar state (protección CSRF)
        if (!callbackHandler.validateState(state, expectedState)) {
            throw new SecurityException("State inválido. Posible ataque CSRF.");
        }

        // 2. Intercambiar code por tokens
        OIDCTokenResponse tokenResponse = callbackHandler.handleCallback(code, state, codeVerifier, redirectUri);

        // 3. Validar ID Token
        Claims idTokenClaims = tokenValidator.validateIdToken(tokenResponse.getIdToken(), expectedNonce);
        String userSub = idTokenClaims.getSubject();

        LOGGER.info("ID Token validado para usuario: " + userSub);

        // 4. Obtener información del usuario desde UserInfo endpoint
        OIDCUserInfo userInfo = userInfoService.getUserInfo(tokenResponse.getAccessToken());

        // 5. Crear o actualizar usuario en la base de datos
        OIDCUser user = createOrUpdateUser(userInfo, tokenResponse);

        // 6. Crear sesión OIDC
        OIDCSession session = createSession(user, tokenResponse);

        // 7. Generar JWT propios de la aplicación
        JWTTokenResponse jwtResponse = generateApplicationJWT(user, session);

        LOGGER.info("Éxito en autenticación completa para usuario: " + userSub);
        return jwtResponse;
    }

    /**
     * Crea o actualiza un usuario en la base de datos
     */
    private OIDCUser createOrUpdateUser(OIDCUserInfo userInfo, OIDCTokenResponse tokenResponse) {
        String sub = userInfo.getSub();

        // Buscar usuario existente
        OIDCUser user = userDAO.findBySub(sub);

        if (user == null) {
            // Crear nuevo usuario
            LOGGER.info("Creando nuevo usuario con sub: " + sub);
            user = new OIDCUser();
            user.setSub(sub);
            user.setCreatedAt(Instant.now());
        } else {
            LOGGER.info("Actualizando usuario existente: " + sub);
        }

        // Actualizar información del usuario
        user.setEmail(userInfo.getEmail());
        user.setEmailVerified(userInfo.getEmailVerified() != null ? userInfo.getEmailVerified() : false);
        user.setFullName(userInfo.getFullName());
        user.setFirstName(userInfo.getFirstName());
        user.setLastName(userInfo.getLastName());
        user.setDocumentType(userInfo.getDocumentType());
        user.setDocumentNumber(userInfo.getDocumentNumber());
        user.setUid(userInfo.getUid());
        user.setRid(userInfo.getRid());
        user.setNid(userInfo.getNid());
        user.setLastLogin(Instant.now());
        user.setActive(true);
        user.setUpdatedAt(Instant.now());

        // Guardar en base de datos
        user = userDAO.save(user);
        LOGGER.info("Usuario guardado con ID: " + user.getId());

        return user;
    }

    /**
     * Crea una sesión OIDC para el usuario
     */
    private OIDCSession createSession(OIDCUser user, OIDCTokenResponse tokenResponse) {
        LOGGER.info("Creando sesión OIDC para usuario: " + user.getSub());

        OIDCSession session = new OIDCSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserSub(user.getSub());
        session.setAccessToken(tokenResponse.getAccessToken());
        session.setIdToken(tokenResponse.getIdToken());
        session.setRefreshToken(tokenResponse.getRefreshToken());
        session.setCreatedAt(Instant.now());

        // Calcular expiración del access token
        if (tokenResponse.getExpiresIn() != null && tokenResponse.getExpiresIn() > 0) {
            session.setExpiresAt(Instant.now().plusSeconds(tokenResponse.getExpiresIn()));
        } else {
            // Default: 1 hora
            session.setExpiresAt(Instant.now().plusSeconds(3600));
        }

        session.setActive(true);

        // Guardar sesión
        session = sessionDAO.save(session);
        LOGGER.info("Sesión creada con ID: " + session.getSessionId());

        return session;
    }

    /**
     * Genera JWT propios de la aplicación para el usuario autenticado
     */
    private JWTTokenResponse generateApplicationJWT(OIDCUser user, OIDCSession session) {
        LOGGER.info("Generando JWT propio de la aplicación para usuario: " + user.getSub());

        // Roles del usuario (por ahora, rol básico "USER")
        List<String> roles = Arrays.asList("USER");

        // Generar access token JWT
        String accessToken = jwtTokenProvider.generateAccessToken(user.getSub(), roles);

        // Generar refresh token JWT
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getSub());

        JWTTokenResponse jwtResponse = new JWTTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600, // expires_in en segundos
                user.getSub(),
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
        String userSub = claims.getSubject();

        // Buscar usuario
        OIDCUser user = userDAO.findBySub(userSub);
        if (user == null || !user.isActive()) {
            throw new SecurityException("Usuario no encontrado o inactivo");
        }

        // Generar nuevos tokens
        List<String> roles = Arrays.asList("USER");
        String newAccessToken = jwtTokenProvider.generateAccessToken(userSub, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userSub);

        JWTTokenResponse jwtResponse = new JWTTokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                3600,
                userSub,
                roles
        );

        LOGGER.info("JWT refrescado exitosamente para usuario: " + userSub);
        return jwtResponse;
    }

    /**
     * Cierra la sesión del usuario
     *
     * @param userSub Subject del usuario
     */
    public void logout(String userSub) {
        LOGGER.info("Cerrando sesión para usuario: " + userSub);

        // Invalidar todas las sesiones OIDC del usuario
        sessionDAO.deleteAllSessionsByUserSub(userSub);

        LOGGER.info("Sesiones cerradas para usuario: " + userSub);
    }

    /**
     * Construye la URL de autorización para redirigir al usuario
     */
    private String buildAuthorizationUrl(String redirectUri, String codeChallenge, String state, String nonce) {
        StringBuilder url = new StringBuilder(oidcConfig.getAuthorizationEndpoint());
        url.append("?response_type=code");
        url.append("&client_id=").append(oidcConfig.getClientId());
        url.append("&redirect_uri=").append(redirectUri);
        url.append("&scope=").append(oidcConfig.getScope());
        url.append("&state=").append(state);
        url.append("&nonce=").append(nonce);
        url.append("&code_challenge=").append(codeChallenge);
        url.append("&code_challenge_method=S256");

        return url.toString();
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
