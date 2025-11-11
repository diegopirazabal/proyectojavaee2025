package hcen.central.frontend.usuariosalud.bean;

import hcen.central.frontend.usuariosalud.dto.ApiResponse;
import hcen.central.frontend.usuariosalud.dto.HistoriaClinicaDocumentoDTO;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Backing bean para la página de historia clínica.
 * Consume el endpoint REST GET /api/historia-clinica/{cedula}/documentos
 */
@Named
@ViewScoped
public class HistoriaClinicaBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(HistoriaClinicaBean.class.getName());

    @Inject
    private UsuarioSaludLoginBean loginBean;

    private List<HistoriaClinicaDocumentoDTO> documentos;
    private String mensajeError;
    private boolean loading;
    private HistoriaClinicaDocumentoDTO documentoSeleccionado;

    @PostConstruct
    public void init() {
        cargarDocumentos();
    }

    public void cargarDocumentos() {
        loading = true;
        mensajeError = null;
        documentos = null;

        String cedula = loginBean.getCedulaUsuarioActual();
        if (cedula == null || cedula.isBlank()) {
            mensajeError = "No se pudo obtener la cédula del usuario autenticado";
            loading = false;
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", mensajeError);
            return;
        }

        String backendUrl = obtenerBackendUrl();
        String endpoint = backendUrl + "/api/historia-clinica/" + cedula + "/documentos";

        LOGGER.info("Consultando historia clínica en: " + endpoint);

        try (Client client = createBackendClient()) {
            Response response = client.target(endpoint)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            LOGGER.info("Respuesta del backend: HTTP " + response.getStatus());

            if (response.getStatus() == 200) {
                // Deserializar ApiResponse que envuelve la lista de documentos
                ApiResponse<List<HistoriaClinicaDocumentoDTO>> apiResponse =
                    response.readEntity(new GenericType<ApiResponse<List<HistoriaClinicaDocumentoDTO>>>() {});

                if (apiResponse != null && apiResponse.isSuccess()) {
                    documentos = apiResponse.getData();

                    if (documentos == null) {
                        documentos = new ArrayList<>();
                    }

                    LOGGER.info("Documentos obtenidos: " + documentos.size());

                    if (documentos.isEmpty()) {
                        addMessage(FacesMessage.SEVERITY_INFO, "Sin documentos",
                                "Aún no hay registros en su historia clínica.");
                    } else {
                        addMessage(FacesMessage.SEVERITY_INFO, "Documentos cargados",
                                "Se encontraron " + documentos.size() + " documento(s) clínico(s).");
                    }
                } else {
                    mensajeError = "Error en la respuesta del servidor";
                    if (apiResponse != null && apiResponse.getError() != null) {
                        mensajeError = apiResponse.getError();
                    }
                    LOGGER.warning(mensajeError);
                    addMessage(FacesMessage.SEVERITY_ERROR, "Error", mensajeError);
                }
            } else if (response.getStatus() == 404) {
                documentos = new ArrayList<>();
                addMessage(FacesMessage.SEVERITY_INFO, "Sin documentos",
                        "No se encontraron documentos para el usuario.");
            } else {
                mensajeError = "Error al cargar documentos (HTTP " + response.getStatus() + ")";
                LOGGER.warning(mensajeError);
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", mensajeError);
            }
        } catch (Exception e) {
            mensajeError = "No pudimos cargar su historia clínica. Intente más tarde.";
            LOGGER.log(Level.SEVERE, "Error consultando historia clínica", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error de conexión", mensajeError);
        } finally {
            loading = false;
        }
    }

    private Client createBackendClient() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String trustAllParam = facesContext.getExternalContext()
                .getInitParameter("hcen.trustAllCertificates");
        boolean trustAll = Boolean.parseBoolean(trustAllParam);

        if (!trustAll) {
            return ClientBuilder.newClient();
        }

        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            // No validation
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // No validation
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            HostnameVerifier permissiveVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            return ClientBuilder.newBuilder()
                    .sslContext(sslContext)
                    .hostnameVerifier(permissiveVerifier)
                    .build();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "No se pudo desactivar la validación SSL, usando configuración por defecto", ex);
            return ClientBuilder.newClient();
        }
    }

    public void refrescar() {
        LOGGER.info("Refrescando historia clínica");
        documentos = null;
        mensajeError = null;
        cargarDocumentos();
    }

    public void seleccionarDocumento(HistoriaClinicaDocumentoDTO documento) {
        this.documentoSeleccionado = documento;
        LOGGER.info("Documento seleccionado: " + documento.getDocumentoId());
    }

    private String obtenerBackendUrl() {
        FacesContext context = FacesContext.getCurrentInstance();
        String backendUrl = context.getExternalContext().getInitParameter("hcen.backendUrl");

        if (backendUrl == null || backendUrl.isBlank()) {
            // Fallback: construir URL basándose en el contexto actual
            String contextPath = context.getExternalContext().getRequestContextPath();
            String serverName = context.getExternalContext().getRequestServerName();
            int serverPort = context.getExternalContext().getRequestServerPort();

            if (serverPort == 80 || serverPort == 443) {
                backendUrl = "http://" + serverName + "/";
            } else {
                backendUrl = "http://" + serverName + ":" + serverPort + "/";
            }

            LOGGER.warning("No se configuró hcen.backendUrl en web.xml, usando fallback: " + backendUrl);
        }

        return backendUrl;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    // Getters y setters

    public List<HistoriaClinicaDocumentoDTO> getDocumentos() {
        return documentos;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public boolean isLoading() {
        return loading;
    }

    public HistoriaClinicaDocumentoDTO getDocumentoSeleccionado() {
        return documentoSeleccionado;
    }

    public void setDocumentoSeleccionado(HistoriaClinicaDocumentoDTO documentoSeleccionado) {
        this.documentoSeleccionado = documentoSeleccionado;
        if (documentoSeleccionado != null) {
            LOGGER.info("Documento seleccionado via setter: " + documentoSeleccionado.getDocumentoId());
        } else {
            LOGGER.warning("setDocumentoSeleccionado llamado con null");
        }
    }

    public boolean isHasDocumentos() {
        return documentos != null && !documentos.isEmpty();
    }
}
