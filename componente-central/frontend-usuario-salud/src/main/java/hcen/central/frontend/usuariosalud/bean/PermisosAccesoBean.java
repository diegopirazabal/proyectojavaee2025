package hcen.central.frontend.usuariosalud.bean;

import hcen.central.frontend.usuariosalud.dto.ApiResponse;
import hcen.central.frontend.usuariosalud.dto.HistoriaClinicaDocumentoDTO;
import hcen.central.frontend.usuariosalud.dto.HistoriaClinicaIdResponse;
import hcen.central.frontend.usuariosalud.dto.PoliticaAccesoDTO;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Serializable;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean para gestionar políticas de acceso de un usuario salud.
 * Permite ver, extender, modificar y revocar permisos de acceso a documentos clínicos.
 */
@Named
@ViewScoped
public class PermisosAccesoBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PermisosAccesoBean.class.getName());
    private static final long serialVersionUID = 1L;

    @Inject
    private UsuarioSaludLoginBean loginBean;

    private String historiaId;
    private List<PoliticaAccesoDTO> permisos = new ArrayList<>();
    private boolean loading = false;
    private String mensajeError;

    // Permiso seleccionado para operaciones
    private PoliticaAccesoDTO permisoSeleccionado;

    // Datos para extender expiración
    private LocalDate nuevaFechaExpiracion;

    // Datos para modificar tipo
    private String nuevoTipoPermiso;
    private Integer nuevoCiProfesional;
    private String nuevaEspecialidad;

    // Datos para revocar
    private String motivoRevocacion;
    private final Map<String, HistoriaClinicaDocumentoDTO> documentosPorId = new HashMap<>();

    @PostConstruct
    public void init() {
        cargarHistoriaId();
        if (historiaId != null) {
            cargarPermisos();
        }
    }

    /**
     * Carga el ID de la historia clínica del usuario autenticado
     */
    private void cargarHistoriaId() {
        String cedula = loginBean.getCedulaUsuarioActual();
        if (cedula == null || cedula.isBlank()) {
            mensajeError = "No se pudo obtener la cédula del usuario autenticado";
            LOGGER.warning("getCedulaUsuarioActual() retornó null o vacío");
            return;
        }

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/historia-clinica/by-cedula/" + cedula;

        try (Client client = createBackendClient()) {
            var requestBuilder = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }
            Response response = requestBuilder.get();

            int status = response.getStatus();
            if (status == 200) {
                ApiResponse<HistoriaClinicaIdResponse> apiResponse =
                        response.readEntity(new GenericType<ApiResponse<HistoriaClinicaIdResponse>>() {});

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    historiaId = apiResponse.getData().getHistoriaId();
                    LOGGER.info("Historia clínica ID obtenido: " + historiaId);
                } else {
                    // Respuesta exitosa pero sin datos (sin documentos clínicos)
                    historiaId = null;
                }
            } else if (status == 500) {
                // 500 = No hay historia clínica para mostrar (sin documentos clínicos)
                historiaId = null;
                LOGGER.info("No hay historia clínica para el usuario (sin documentos clínicos)");
            } else if (status == 404) {
                // 404 = Error al conseguir la historia clínica
                mensajeError = "Hubo un error al conseguir la historia clínica";
            } else {
                mensajeError = "Error al obtener historia clínica (código " + status + ")";
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar historia ID", e);
            mensajeError = "Error de comunicación con el servidor: " + e.getMessage();
        }
    }

    /**
     * Carga la lista de permisos de la historia clínica
     */
    public void cargarPermisos() {
        if (historiaId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se ha cargado el ID de historia clínica");
            return;
        }

        loading = true;
        mensajeError = null;
        permisos = new ArrayList<>();

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/politicas-acceso/historia/" + historiaId;

        try (Client client = createBackendClient()) {
            var requestBuilder = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }
            Response response = requestBuilder.get();

            int status = response.getStatus();
            if (status == 200) {
                ApiResponse<List<PoliticaAccesoDTO>> apiResponse =
                        response.readEntity(new GenericType<ApiResponse<List<PoliticaAccesoDTO>>>() {});

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    permisos = apiResponse.getData();
                    LOGGER.info("Permisos cargados: " + permisos.size());
                    cargarDocumentosHistoria();
                    enriquecerPermisosConDocumentos();

                    if (permisos.isEmpty()) {
                        addMessage(FacesMessage.SEVERITY_INFO, "Sin permisos",
                                "No hay permisos de acceso registrados para su historia clínica");
                    }
                } else {
                    mensajeError = "Error en la respuesta del servidor";
                }
            } else {
                mensajeError = "Error al cargar permisos (código " + status + ")";
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar permisos", e);
            mensajeError = "Error de comunicación: " + e.getMessage();
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudieron cargar los permisos");
        } finally {
            loading = false;
        }
    }

    private void cargarDocumentosHistoria() {
        documentosPorId.clear();
        String cedula = loginBean.getCedulaUsuarioActual();
        if (cedula == null || cedula.isBlank()) {
            return;
        }

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/historia-clinica/" + cedula + "/documentos";

        try (Client client = createBackendClient()) {
            var requestBuilder = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }
            Response response = requestBuilder.get();

            if (response.getStatus() == 200) {
                ApiResponse<List<HistoriaClinicaDocumentoDTO>> apiResponse =
                        response.readEntity(new GenericType<ApiResponse<List<HistoriaClinicaDocumentoDTO>>>() {});
                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    for (HistoriaClinicaDocumentoDTO doc : apiResponse.getData()) {
                        if (doc.getDocumentoId() != null) {
                            documentosPorId.put(doc.getDocumentoId(), doc);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudieron cargar los documentos de historia clínica", e);
        }
    }

    private void enriquecerPermisosConDocumentos() {
        if (permisos == null || permisos.isEmpty()) {
            return;
        }
        for (PoliticaAccesoDTO permiso : permisos) {
            if (permiso.getDocumentoId() == null) {
                continue;
            }
            HistoriaClinicaDocumentoDTO doc = documentosPorId.get(permiso.getDocumentoId());
            if (doc == null) {
                continue;
            }
            permiso.setMotivoDocumento(doc.getMotivoDisplay());
            permiso.setNombreClinica(doc.getClinicaDisplay());
            permiso.setFechaDocumento(doc.getFechaFormateada());
            permiso.setFechaRegistroDocumento(doc.getFechaRegistroFormateada());
        }
    }

    /**
     * Prepara el diálogo para extender la expiración
     */
    public void prepararExtenderExpiracion(PoliticaAccesoDTO permiso) {
        if (permiso == null || !permiso.isActivo()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Solo se pueden extender permisos activos");
            return;
        }

        this.permisoSeleccionado = permiso;
        // Por defecto, sugerir 15 días más desde hoy
        this.nuevaFechaExpiracion = LocalDate.now().plusDays(15);
    }

    /**
     * Extiende la fecha de expiración del permiso seleccionado
     */
    public void extenderExpiracion() {
        if (permisoSeleccionado == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No hay permiso seleccionado");
            return;
        }

        if (nuevaFechaExpiracion == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Debe especificar la nueva fecha de expiración");
            return;
        }

        if (nuevaFechaExpiracion.isBefore(LocalDate.now())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "La fecha debe ser futura");
            return;
        }

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/politicas-acceso/" + permisoSeleccionado.getId() + "/extender";

        try (Client client = createBackendClient()) {
            // Convertir LocalDate a LocalDateTime (inicio del día)
            String fechaStr = nuevaFechaExpiracion.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            JsonObject requestBody = Json.createObjectBuilder()
                    .add("nuevaFechaExpiracion", fechaStr)
                    .build();

            var requestBuilder = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }
            Response response = requestBuilder.put(Entity.json(requestBody.toString()));

            int status = response.getStatus();
            if (status == 200) {
                addMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Fecha de expiración extendida correctamente");
                cargarPermisos(); // Recargar lista
            } else {
                String errorMsg = "No se pudo extender la expiración (código " + status + ")";
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", errorMsg);
                LOGGER.warning(errorMsg);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al extender expiración", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error de comunicación: " + e.getMessage());
        }
    }

    /**
     * Prepara el diálogo para modificar el tipo de permiso
     */
    public void prepararModificarTipo(PoliticaAccesoDTO permiso) {
        if (permiso == null || !permiso.isActivo()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Solo se pueden modificar permisos activos");
            return;
        }

        this.permisoSeleccionado = permiso;
        this.nuevoTipoPermiso = permiso.getTipoPermiso();
        this.nuevoCiProfesional = permiso.getCiProfesional();
        this.nuevaEspecialidad = permiso.getEspecialidad();
    }

    /**
     * Modifica el tipo de permiso del permiso seleccionado
     */
    public void modificarTipo() {
        if (permisoSeleccionado == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No hay permiso seleccionado");
            return;
        }

        if (nuevoTipoPermiso == null || nuevoTipoPermiso.isBlank()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Debe seleccionar el tipo de permiso");
            return;
        }

        // Validaciones según tipo
        if ("PROFESIONAL_ESPECIFICO".equals(nuevoTipoPermiso) && nuevoCiProfesional == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Debe especificar la CI del profesional");
            return;
        }

        if ("POR_ESPECIALIDAD".equals(nuevoTipoPermiso) &&
                (nuevaEspecialidad == null || nuevaEspecialidad.isBlank())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Debe especificar la especialidad");
            return;
        }

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/politicas-acceso/" + permisoSeleccionado.getId() + "/modificar-tipo";

        try (Client client = createBackendClient()) {
            var builder = Json.createObjectBuilder()
                    .add("tipoPermiso", nuevoTipoPermiso);

            if (nuevoCiProfesional != null) {
                builder.add("ciProfesional", nuevoCiProfesional);
            }
            if (nuevaEspecialidad != null && !nuevaEspecialidad.isBlank()) {
                builder.add("especialidad", nuevaEspecialidad);
            }

            var requestBuilder = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }
            Response response = requestBuilder.put(Entity.json(builder.build().toString()));

            int status = response.getStatus();
            if (status == 200) {
                addMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Tipo de permiso modificado correctamente");
                cargarPermisos();
            } else {
                String errorMsg = "No se pudo modificar el tipo de permiso (código " + status + ")";
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", errorMsg);
                LOGGER.warning(errorMsg);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al modificar tipo de permiso", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error de comunicación: " + e.getMessage());
        }
    }

    /**
     * Prepara el diálogo para revocar un permiso
     */
    public void prepararRevocar(PoliticaAccesoDTO permiso) {
        if (permiso == null || !permiso.isActivo()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Solo se pueden revocar permisos activos");
            return;
        }

        this.permisoSeleccionado = permiso;
        this.motivoRevocacion = "";
    }

    /**
     * Revoca el permiso seleccionado
     */
    public void revocarPermiso() {
        if (permisoSeleccionado == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No hay permiso seleccionado");
            return;
        }

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/politicas-acceso/" + permisoSeleccionado.getId() + "/revocar";

        try (Client client = createBackendClient()) {
            var builder = Json.createObjectBuilder();
            if (motivoRevocacion != null && !motivoRevocacion.isBlank()) {
                builder.add("motivo", motivoRevocacion);
            }

            var requestBuilder = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON);
            String jwtToken = getJwtTokenFromCookie();
            if (jwtToken != null && !jwtToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
            }
            Response response = requestBuilder.put(Entity.json(builder.build().toString()));

            int status = response.getStatus();
            if (status == 200) {
                addMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Permiso revocado correctamente");
                cargarPermisos();
            } else {
                String errorMsg = "No se pudo revocar el permiso (código " + status + ")";
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", errorMsg);
                LOGGER.warning(errorMsg);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al revocar permiso", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error de comunicación: " + e.getMessage());
        }
    }

    // Métodos auxiliares

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    /**
     * Obtiene el JWT token de la cookie
     */
    private String getJwtTokenFromCookie() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al obtener JWT de cookie", e);
        }
        return null;
    }

    private String obtenerBackendUrl() {
        FacesContext context = FacesContext.getCurrentInstance();
        String backendUrl = context.getExternalContext()
                .getInitParameter("hcen.backendUrl");

        if (backendUrl == null || backendUrl.isBlank()) {
            String serverName = context.getExternalContext().getRequestServerName();
            int serverPort = context.getExternalContext().getRequestServerPort();
            backendUrl = "http://" + serverName + ":" + serverPort;
        }

        return backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;
    }

    private Client createBackendClient() {
        String trustAllParam = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getInitParameter("hcen.trustAllCertificates");
        boolean trustAll = Boolean.parseBoolean(trustAllParam);

        if (!trustAll) {
            return ClientBuilder.newClient();
        }

        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            return ClientBuilder.newBuilder()
                    .sslContext(sslContext)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo crear cliente con SSL permisivo, usando default", e);
            return ClientBuilder.newClient();
        }
    }

    // Getters y Setters

    public List<PoliticaAccesoDTO> getPermisos() {
        return permisos;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public boolean getHasPermisos() {
        return permisos != null && !permisos.isEmpty();
    }

    public boolean getHasMensajeError() {
        return mensajeError != null && !mensajeError.isBlank();
    }

    public PoliticaAccesoDTO getPermisoSeleccionado() {
        return permisoSeleccionado;
    }

    public void setPermisoSeleccionado(PoliticaAccesoDTO permisoSeleccionado) {
        this.permisoSeleccionado = permisoSeleccionado;
    }

    public LocalDate getNuevaFechaExpiracion() {
        return nuevaFechaExpiracion;
    }

    public void setNuevaFechaExpiracion(LocalDate nuevaFechaExpiracion) {
        this.nuevaFechaExpiracion = nuevaFechaExpiracion;
    }

    public String getNuevoTipoPermiso() {
        return nuevoTipoPermiso;
    }

    public void setNuevoTipoPermiso(String nuevoTipoPermiso) {
        this.nuevoTipoPermiso = nuevoTipoPermiso;
    }

    public Integer getNuevoCiProfesional() {
        return nuevoCiProfesional;
    }

    public void setNuevoCiProfesional(Integer nuevoCiProfesional) {
        this.nuevoCiProfesional = nuevoCiProfesional;
    }

    public String getNuevaEspecialidad() {
        return nuevaEspecialidad;
    }

    public void setNuevaEspecialidad(String nuevaEspecialidad) {
        this.nuevaEspecialidad = nuevaEspecialidad;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public void setMotivoRevocacion(String motivoRevocacion) {
        this.motivoRevocacion = motivoRevocacion;
    }
}
