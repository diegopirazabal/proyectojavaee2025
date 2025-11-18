package hcen.central.frontend.usuariosalud.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@SessionScoped
public class UsuarioSaludLoginBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludLoginBean.class.getName());

    private static final String TEMP_USERNAME = "user";
    private static final String TEMP_PASSWORD = "user";
    private static final String DEFAULT_DOC_TYPE = "OTRO";
    private static final String DEFAULT_DOC_NUMBER = "85335898";

    private String username;
    private String password;
    private boolean loggedIn;
    private String cedulaUsuarioActual;

    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (credencialesValidas()) {
            loggedIn = true;
            cedulaUsuarioActual = DEFAULT_DOC_NUMBER;
            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Bienvenido", "Consulta habilitada para usuario temporal"));
            return construirResultadoNavegacionDashboard();
        }

        loggedIn = false;
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Credenciales inválidas", "Usuario o contraseña incorrectos"));
        return null;
    }

    public String logout() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext external = context.getExternalContext();
        loggedIn = false;
        username = null;
        password = null;
        cedulaUsuarioActual = null;
        external.invalidateSession();
        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Sesión cerrada", "Has finalizado la consulta"));
        return "/login?faces-redirect=true";
    }

    public void checkAuthentication() throws IOException {
        // Verificar si hay cookie JWT (sesión OIDC) antes de redirigir
        if (!loggedIn && !hasOidcSession()) {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            external.getFlash().setRedirect(true); // evitar NPE de JSF al redirigir desde preRenderView
            external.redirect(external.getRequestContextPath() + "/login.xhtml");
            FacesContext.getCurrentInstance().responseComplete();
        } else if (!loggedIn && hasOidcSession()) {
            // Marcar como logueado si hay cookie JWT válida
            loggedIn = true;
            // Intentar obtener cédula de OIDC
            obtenerCedulaDeOIDC();
        }
    }

    public void redirectIfLoggedIn() throws IOException {
        if (loggedIn) {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            external.getFlash().setRedirect(true); // garantizar que el flash conoce la redirección
            external.redirect(external.getRequestContextPath() + construirRutaDashboard());
            FacesContext.getCurrentInstance().responseComplete();
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCedulaUsuarioActual() {
        return cedulaUsuarioActual;
    }

    private boolean credencialesValidas() {
        return TEMP_USERNAME.equals(username) && TEMP_PASSWORD.equals(password);
    }

    private String construirResultadoNavegacionDashboard() {
        return "dashboard?faces-redirect=true&docType=%s&docNumber=%s".formatted(
                codificar(DEFAULT_DOC_TYPE),
                codificar(DEFAULT_DOC_NUMBER)
        );
    }

    private String construirRutaDashboard() {
        return "/dashboard.xhtml?docType=%s&docNumber=%s".formatted(
                codificar(DEFAULT_DOC_TYPE),
                codificar(DEFAULT_DOC_NUMBER)
        );
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private boolean hasOidcSession() {
        try {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();

            // Primero verificar cookie JWT (más confiable entre WARs)
            HttpServletRequest request = (HttpServletRequest) external.getRequest();
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        return true;
                    }
                }
            }

            // Fallback: verificar userInfo en sesión HTTP (mismo WAR)
            HttpSession session = (HttpSession) external.getSession(false);
            return session != null && session.getAttribute("userInfo") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void obtenerCedulaDeOIDC() {
        try {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            HttpServletRequest request = (HttpServletRequest) external.getRequest();

            // 1. PRIORIDAD MÁXIMA: Extraer cédula del JWT en la cookie
            String cedulaFromJwt = extractCedulaFromJwtCookie(request);
            if (cedulaFromJwt != null && !cedulaFromJwt.isBlank()) {
                cedulaUsuarioActual = cedulaFromJwt;
                LOGGER.info("Cédula obtenida del JWT: " + cedulaFromJwt);
                return;
            }

            // 2. Fallback: Intentar obtener de parámetros URL (viene del callback inicial)
            String docNumber = external.getRequestParameterMap().get("docNumber");
            if (docNumber != null && !docNumber.isBlank()) {
                cedulaUsuarioActual = docNumber;
                LOGGER.info("Cédula obtenida de parámetros URL: " + docNumber);
                return;
            }

            // 3. Fallback: intentar obtener de la sesión HTTP (mismo WAR, raro)
            HttpSession session = (HttpSession) external.getSession(false);
            if (session != null) {
                Object userInfoObj = session.getAttribute("userInfo");
                if (userInfoObj != null) {
                    String cedula = (String) userInfoObj.getClass().getMethod("getNumeroDocumento").invoke(userInfoObj);
                    if (cedula != null && !cedula.isBlank()) {
                        cedulaUsuarioActual = cedula;
                        LOGGER.info("Cédula obtenida de sesión HTTP: " + cedula);
                        return;
                    }
                }
            }

            // 4. Último fallback: usar valor por defecto (solo para desarrollo)
            LOGGER.warning("No se pudo obtener cédula de OIDC, usando default");
            cedulaUsuarioActual = DEFAULT_DOC_NUMBER;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo cédula de OIDC", e);
            cedulaUsuarioActual = DEFAULT_DOC_NUMBER;
        }
    }

    /**
     * Extrae la cédula del JWT almacenado en la cookie jwt_token
     * Decodifica el payload del JWT y obtiene el claim "sub" (subject)
     *
     * @param request HttpServletRequest con las cookies
     * @return Cédula del usuario o null si no se puede extraer
     */
    private String extractCedulaFromJwtCookie(HttpServletRequest request) {
        try {
            // 1. Buscar cookie jwt_token
            String jwtToken = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName())) {
                        jwtToken = cookie.getValue();
                        break;
                    }
                }
            }

            if (jwtToken == null || jwtToken.isBlank()) {
                LOGGER.fine("No se encontró cookie jwt_token");
                return null;
            }

            // 2. El JWT tiene formato: header.payload.signature
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                LOGGER.warning("JWT con formato inválido (no tiene 3 partes)");
                return null;
            }

            // 3. Decodificar el payload (segunda parte) de Base64URL
            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);

            LOGGER.fine("JWT payload decodificado: " + payloadJson);

            // 4. Parsear JSON manualmente para obtener "sub"
            // Formato esperado: {"iss":"g2.hcen.uy","sub":"12345678","exp":...}
            String cedula = extractJsonField(payloadJson, "sub");

            if (cedula != null && !cedula.isBlank()) {
                LOGGER.info("Cédula extraída del JWT: " + cedula);
                return cedula;
            } else {
                LOGGER.warning("No se encontró claim 'sub' en el JWT");
                return null;
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extrayendo cédula del JWT", e);
            return null;
        }
    }

    /**
     * Extrae un campo de un JSON simple (sin usar librerías)
     * Formato esperado: {"key":"value",...}
     *
     * @param json String JSON
     * @param fieldName Nombre del campo a extraer
     * @return Valor del campo o null
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            String searchKey = "\"" + fieldName + "\"";
            int keyIndex = json.indexOf(searchKey);

            if (keyIndex == -1) {
                return null;
            }

            // Buscar el ":" después de la clave
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            // Saltar espacios y comillas
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() &&
                   (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
                valueStart++;
            }

            // Buscar el final del valor (comilla o coma o })
            int valueEnd = valueStart;
            while (valueEnd < json.length() &&
                   json.charAt(valueEnd) != '"' &&
                   json.charAt(valueEnd) != ',' &&
                   json.charAt(valueEnd) != '}') {
                valueEnd++;
            }

            String value = json.substring(valueStart, valueEnd).trim();
            return value.isEmpty() ? null : value;

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error parseando campo JSON: " + fieldName, e);
            return null;
        }
    }

    public String getOidcLoginUrl() {
        // redirect_uri DEBE ser fija y estar registrada en gub.uy
        ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
        String serverName = external.getRequestServerName();

        // Determinar si es producción o desarrollo
        boolean isProduction = "hcen-uy.web.elasticloud.uy".equals(serverName);
        String redirectUri;
        String baseUrl;

        if (isProduction) {
            // Producción
            baseUrl = "https://hcen-uy.web.elasticloud.uy";
            redirectUri = "https://hcen-uy.web.elasticloud.uy/api/auth/callback";
        } else {
            // Desarrollo - backend en /hcen-central
            baseUrl = "http://localhost:8080/hcen-central";
            redirectUri = "http://localhost:8080/hcen-central/api/auth/callback";
        }

        return baseUrl + "/api/auth/login?redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&origin=usuario-salud";
    }
}
