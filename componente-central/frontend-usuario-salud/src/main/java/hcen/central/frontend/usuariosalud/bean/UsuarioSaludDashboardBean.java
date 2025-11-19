package hcen.central.frontend.usuariosalud.bean;

import com.hcen.periferico.usuariosalud.util.JwtUtil;
import hcen.central.frontend.usuariosalud.dto.CiudadanoDetalle;
import hcen.central.frontend.usuariosalud.dto.UsuarioSaludDTO;
import hcen.central.frontend.usuariosalud.service.APIService;
import hcen.central.frontend.usuariosalud.service.DnicServiceClient;
import hcen.central.frontend.usuariosalud.service.DocumentoNoEncontradoException;
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
    private APIService apiService;

    @Inject
    private UsuarioSaludLoginBean loginBean;

    private UsuarioSaludDTO usuarioDB;  // Datos del usuario desde la base de datos
    private CiudadanoDetalle ciudadano;  // Datos complementarios del DNIC (sexo, nacionalidad)
    private String docType;
    private String docNumber;
    private String warningMessage;
    private Boolean notificacionesHabilitadas = Boolean.TRUE;

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

        // Extraer docType y docNumber del JWT si existe
        if (hasJwtCookie) {
            LOGGER.info("Cookie JWT encontrada, extrayendo datos del usuario");
            String jwtDocType = JwtUtil.extractDocType(jwtToken);
            String jwtDocNumber = JwtUtil.extractDocNumber(jwtToken);
            
            if (jwtDocType != null && jwtDocNumber != null) {
                docType = jwtDocType;
                docNumber = jwtDocNumber;
                LOGGER.info("Datos del JWT - docType: " + docType + ", docNumber: " + docNumber);
            }
        } else {
            LOGGER.info("Sesión local autenticada sin cookie JWT, permitiendo acceso");
        }

        // Verificar sesión HTTP para advertencias
        HttpSession session = (HttpSession) externalContext.getSession(false);
        if (session != null) {
            // Mostrar advertencia de menor de edad si viene en el JWT de sesión
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
        }

        // Fallback: Si no hay datos del JWT, intentar LoginBean
        if ((docType == null || docType.isBlank()) && loginBean != null && loginBean.getCedulaUsuarioActual() != null) {
            docType = "OTRO";
            docNumber = loginBean.getCedulaUsuarioActual();
            LOGGER.info("Usando cédula del LoginBean: " + docNumber);
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

        // Paso 1: Cargar datos principales desde la base de datos
        try {
            usuarioDB = apiService.obtenerUsuario(numero);
            if (usuarioDB != null) {
                LOGGER.info("Datos del usuario cargados desde BD para cédula: " + numero);

                // Inicializar ciudadano con datos de la BD
                ciudadano = new CiudadanoDetalle();
                ciudadano.setTipoDocumento(usuarioDB.getTipoDocumento() != null ? usuarioDB.getTipoDocumento() : tipo);
                ciudadano.setNumeroDocumento(numero);
                ciudadano.setPrimerNombre(usuarioDB.getPrimerNombre());
                ciudadano.setSegundoNombre(usuarioDB.getSegundoNombre());
                ciudadano.setPrimerApellido(usuarioDB.getPrimerApellido());
                ciudadano.setSegundoApellido(usuarioDB.getSegundoApellido());
                ciudadano.setEmail(usuarioDB.getEmail() != null ? usuarioDB.getEmail() : "");
                ciudadano.setTelefono(usuarioDB.getTelefono() != null ? usuarioDB.getTelefono() : "");
                ciudadano.setDireccion(usuarioDB.getDireccion() != null ? usuarioDB.getDireccion() : "");
                notificacionesHabilitadas = usuarioDB.getNotificacionesHabilitadas() != null
                        ? usuarioDB.getNotificacionesHabilitadas()
                        : Boolean.TRUE;

                // Formatear fecha de nacimiento para mostrar
                if (usuarioDB.getFechaNacimiento() != null) {
                    ciudadano.setFechaNacimiento(usuarioDB.getFechaNacimiento().toString());
                    updateWarningFromBirthDate(usuarioDB.getFechaNacimiento().toString());
                }

                // Paso 2: Complementar con datos demográficos del DNIC (sexo, nacionalidad)
                try {
                    CiudadanoDetalle datosDnic = dnicServiceClient.obtenerCiudadano(tipo, numero);
                    // Solo complementar campos que no están en la BD
                    ciudadano.setSexo(datosDnic.getSexo());
                    ciudadano.setCodigoNacionalidad(datosDnic.getCodigoNacionalidad());
                    ciudadano.setNombreEnCedula(datosDnic.getNombreEnCedula());
                    LOGGER.info("Datos demográficos complementados desde DNIC");
                } catch (DocumentoNoEncontradoException e) {
                    LOGGER.warning("No se encontraron datos en DNIC para complementar: " + e.getMessage());
                    // No es crítico, continuar con datos de la BD
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al consultar DNIC para complementar datos", e);
                    // No es crítico, continuar con datos de la BD
                }

                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Datos cargados", "Información obtenida desde el sistema"));
            } else {
                // Usuario no existe en la BD, intentar consultar solo DNIC (comportamiento legacy)
                LOGGER.info("Usuario no encontrado en BD, consultando solo DNIC");
                cargarSoloDesdeDnic(tipo, numero, context);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del usuario desde BD", e);
            // Fallback: intentar cargar solo desde DNIC
            cargarSoloDesdeDnic(tipo, numero, context);
        }

        docType = tipo;
        docNumber = numero;
    }

    /**
     * Método auxiliar para cargar datos solo desde DNIC (comportamiento legacy/fallback)
     */
    private void cargarSoloDesdeDnic(String tipo, String numero, FacesContext context) {
        try {
            CiudadanoDetalle respuesta = dnicServiceClient.obtenerCiudadano(tipo, numero);
            ciudadano = respuesta;
            usuarioDB = null;  // No hay datos en BD
            notificacionesHabilitadas = Boolean.TRUE;

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
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Usuario no registrado", "Información obtenida solo desde DNIC. El usuario no está registrado en el sistema."));
        } catch (DocumentoNoEncontradoException e) {
            ciudadano = new CiudadanoDetalle();
            usuarioDB = null;
            notificacionesHabilitadas = Boolean.TRUE;
            ciudadano.setTipoDocumento(tipo);
            ciudadano.setNumeroDocumento(numero);
            warningMessage = null;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Documento no encontrado", "No se encontró el documento en DNIC ni en el sistema"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error consultando DNIC", e);
            ciudadano = new CiudadanoDetalle();
            usuarioDB = null;
            notificacionesHabilitadas = Boolean.TRUE;
            ciudadano.setTipoDocumento(tipo);
            ciudadano.setNumeroDocumento(numero);
            warningMessage = null;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al consultar DNIC", "No se pudo obtener la información del ciudadano"));
        }
    }

    /**
     * Guarda los cambios realizados a los datos de contacto del usuario
     */
    public void guardarCambios() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (usuarioDB == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "No hay datos de usuario para actualizar"));
            return;
        }

        // Validar email
        if (ciudadano.getEmail() == null || ciudadano.getEmail().trim().isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error de validación", "El email es requerido"));
            return;
        }

        try {
            // Actualizar usuario en la BD
            UsuarioSaludDTO actualizado = apiService.actualizarUsuario(
                usuarioDB.getCedula(),
                ciudadano.getEmail(),
                ciudadano.getTelefono(),
                ciudadano.getDireccion(),
                getNotificacionesHabilitadas()
            );

            // Actualizar datos locales
            usuarioDB = actualizado;
            ciudadano.setEmail(actualizado.getEmail());
            ciudadano.setTelefono(actualizado.getTelefono() != null ? actualizado.getTelefono() : "");
            ciudadano.setDireccion(actualizado.getDireccion() != null ? actualizado.getDireccion() : "");
            notificacionesHabilitadas = actualizado.getNotificacionesHabilitadas() != null
                    ? actualizado.getNotificacionesHabilitadas()
                    : Boolean.TRUE;

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Éxito", "Los datos de contacto se actualizaron correctamente"));
            LOGGER.info("Datos de contacto actualizados para usuario: " + usuarioDB.getCedula());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar datos del usuario", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "No se pudieron guardar los cambios: " + e.getMessage()));
        }
    }

    /**
     * Permite activar o desactivar las notificaciones push para el usuario.
     */
    public void actualizarPreferenciasNotificaciones() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (usuarioDB == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Acción no disponible", "Registra tus datos en el sistema antes de gestionar notificaciones."));
            return;
        }

        Boolean estadoSolicitado = getNotificacionesHabilitadas();
        try {
            UsuarioSaludDTO actualizado = apiService.actualizarUsuario(
                usuarioDB.getCedula(),
                ciudadano.getEmail(),
                ciudadano.getTelefono(),
                ciudadano.getDireccion(),
                estadoSolicitado
            );

            usuarioDB = actualizado;
            notificacionesHabilitadas = actualizado.getNotificacionesHabilitadas() != null
                    ? actualizado.getNotificacionesHabilitadas()
                    : Boolean.TRUE;

            String detalle = Boolean.TRUE.equals(notificacionesHabilitadas)
                    ? "Las notificaciones móviles fueron habilitadas."
                    : "Las notificaciones móviles fueron desactivadas.";
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Preferencias actualizadas", detalle));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar preferencias de notificaciones", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "No se pudieron actualizar las notificaciones. Intenta nuevamente."));
            notificacionesHabilitadas = usuarioDB.getNotificacionesHabilitadas() != null
                    ? usuarioDB.getNotificacionesHabilitadas()
                    : Boolean.TRUE;
        }
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

    public UsuarioSaludDTO getUsuarioDB() {
        return usuarioDB;
    }

    public boolean isUsuarioRegistrado() {
        return usuarioDB != null;
    }

    public Boolean getNotificacionesHabilitadas() {
        return notificacionesHabilitadas != null ? notificacionesHabilitadas : Boolean.TRUE;
    }

    public void setNotificacionesHabilitadas(Boolean notificacionesHabilitadas) {
        this.notificacionesHabilitadas = notificacionesHabilitadas;
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
