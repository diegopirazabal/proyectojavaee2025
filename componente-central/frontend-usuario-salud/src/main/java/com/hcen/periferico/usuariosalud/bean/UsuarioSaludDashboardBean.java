package com.hcen.periferico.usuariosalud.bean;

import com.hcen.periferico.usuariosalud.dto.CiudadanoDetalle;
import com.hcen.periferico.usuariosalud.service.DnicServiceClient;
import com.hcen.periferico.usuariosalud.service.DocumentoNoEncontradoException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@ViewScoped
public class UsuarioSaludDashboardBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludDashboardBean.class.getName());
    private static final long serialVersionUID = 1L;
    private static final ZoneId URUGUAY_ZONE = ZoneId.of("America/Montevideo");

    @Inject
    private DnicServiceClient dnicServiceClient;

    @Inject
    private UsuarioSaludLoginBean loginBean;

    private CiudadanoDetalle ciudadano;
    private String docType;
    private String docNumber;
    private String warningMessage;

    @PostConstruct
    public void init() {
        ciudadano = new CiudadanoDetalle();
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        
        // Verificar autenticación OIDC mediante cookie JWT
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        String jwtToken = getJwtFromCookie(request);
        boolean hasJwtCookie = jwtToken != null && !jwtToken.isBlank();
        boolean hasSessionLogin = loginBean != null && loginBean.isLoggedIn();

        if (!hasJwtCookie && !hasSessionLogin) {
            LOGGER.warning("No se encontró cookie JWT ni sesión activa, redirigiendo a login");
            try {
                externalContext.redirect(externalContext.getRequestContextPath() + "/login.xhtml");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error redirigiendo a login", e);
            }
            return;
        }
        
        if (hasJwtCookie) {
            LOGGER.info("Cookie JWT encontrada, usuario autenticado");
        } else {
            LOGGER.info("Sesión local autenticada sin cookie JWT, permitiendo acceso");
        }
        
        // Verificar sesión HTTP para datos de userInfo (opcional, puede no existir en este WAR)
        HttpSession session = (HttpSession) externalContext.getSession(false);
        
        // Intentar obtener datos de la sesión OIDC primero
        if (session != null) {
            // Mostrar advertencia de menor de edad si viene en el JWT
            Object jwtTokenObj = session.getAttribute("jwtToken");
            if (jwtTokenObj != null) {
                try {
                    Object warning = jwtTokenObj.getClass().getMethod("getWarningMessage").invoke(jwtTokenObj);
                    if (warning instanceof String warningText && !warningText.isBlank()) {
                        showWarning(warningText);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "No se pudo obtener advertencia del JWT", e);
                }
            }

            // Buscar userInfo en la sesión (viene del callback OIDC)
            Object userInfoObj = session.getAttribute("userInfo");
            if (userInfoObj != null) {
                try {
                    // Usar reflexión para obtener la cédula sin depender de la clase OIDCUserInfo
                    String cedula = (String) userInfoObj.getClass().getMethod("getNumeroDocumento").invoke(userInfoObj);
                    if (cedula != null && !cedula.isBlank()) {
                        docType = "CI";
                        docNumber = cedula;
                        LOGGER.info("Cédula obtenida de sesión OIDC: " + cedula);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "No se pudo obtener cédula de userInfo en sesión", e);
                }
            }
        }
        
        // Si no hay datos de OIDC, intentar obtener de parámetros de URL
        if (docType == null || docType.isBlank()) {
            Map<String, String> params = externalContext.getRequestParameterMap();
            docType = params.getOrDefault("docType", "");
            docNumber = params.getOrDefault("docNumber", "");
        }

        if (!docType.isBlank() && !docNumber.isBlank()) {
            buscar();
        }
    }

    public void buscar() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (docType == null || docType.isBlank() || docNumber == null || docNumber.isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Datos incompletos", "Debe indicar el tipo y número de documento"));
            return;
        }

        String tipo = docType.trim().toUpperCase();
        String numero = docNumber.trim();

        ciudadano.setTipoDocumento(tipo);
        ciudadano.setNumeroDocumento(numero);

        try {
            CiudadanoDetalle respuesta = dnicServiceClient.obtenerCiudadano(tipo, numero);
            ciudadano = respuesta;
            LOGGER.info(() -> "Fecha de nacimiento recibida de DNIC para " + numero + ": '" + ciudadano.getFechaNacimiento() + "'");
            if (ciudadano.getEmail() == null) {
                ciudadano.setEmail("");
            }
            if (ciudadano.getTelefono() == null) {
                ciudadano.setTelefono("");
            }
            if (ciudadano.getDireccion() == null) {
                ciudadano.setDireccion("");
            }
            updateWarningFromBirthDate(ciudadano.getFechaNacimiento());
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Datos cargados", "Información obtenida desde DNIC"));
        } catch (DocumentoNoEncontradoException e) {
            ciudadano = new CiudadanoDetalle();
            ciudadano.setTipoDocumento(tipo);
            ciudadano.setNumeroDocumento(numero);
            warningMessage = null;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Documento no se encuentra en DNIC", e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error consultando DNIC", e);
            ciudadano = new CiudadanoDetalle();
            ciudadano.setTipoDocumento(tipo);
            ciudadano.setNumeroDocumento(numero);
            warningMessage = null;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al consultar DNIC", "No se pudo obtener la información del ciudadano"));
        }

        docType = tipo;
        docNumber = numero;
    }

    private void updateWarningFromBirthDate(String fechaNacimiento) {
        if (esMenorDeEdad(fechaNacimiento)) {
            showWarning("Advertencia: el usuario es menor de edad, verifique permisos de acceso.");
        } else {
            warningMessage = null;
        }
    }

    public CiudadanoDetalle getCiudadano() {
        if (ciudadano == null) {
            ciudadano = new CiudadanoDetalle();
        }
        return ciudadano;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }
    
    /**
     * Extrae el JWT de la cookie jwt_token
     */
    private String getJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean esMenorDeEdad(String fechaNacimientoBruta) {
        if (fechaNacimientoBruta == null || fechaNacimientoBruta.isBlank()) {
            return false;
        }
        try {
            LocalDate birthDate = parseFechaNacimiento(fechaNacimientoBruta.trim());
            LocalDate today = LocalDate.now(URUGUAY_ZONE);
            int age = Period.between(birthDate, today).getYears();
            LOGGER.info(() -> "Edad calculada para fecha '" + fechaNacimientoBruta + "' con referencia " + today + ": " + age);
            return age < 18;
        } catch (DateTimeParseException e) {
            LOGGER.log(Level.FINE, "No se pudo interpretar la fecha de nacimiento: {0}", fechaNacimientoBruta);
            return false;
        }
    }

    private void showWarning(String message) {
        warningMessage = message;
    }

    private LocalDate parseFechaNacimiento(String raw) {
        DateTimeParseException lastError = null;
        for (DateTimeFormatter formatter : new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyyMMdd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        }) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ex) {
                lastError = ex;
            }
        }
        throw lastError != null ? lastError : new DateTimeParseException("Formato desconocido", raw, 0);
    }
}
