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
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@ViewScoped
public class UsuarioSaludDashboardBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludDashboardBean.class.getName());
    private static final long serialVersionUID = 1L;

    @Inject
    private DnicServiceClient dnicServiceClient;

    private CiudadanoDetalle ciudadano;
    private String docType;
    private String docNumber;

    @PostConstruct
    public void init() {
        ciudadano = new CiudadanoDetalle();
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        
        // Intentar obtener datos de la sesión OIDC primero
        HttpSession session = (HttpSession) externalContext.getSession(false);
        if (session != null) {
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
            if (ciudadano.getEmail() == null) {
                ciudadano.setEmail("");
            }
            if (ciudadano.getTelefono() == null) {
                ciudadano.setTelefono("");
            }
            if (ciudadano.getDireccion() == null) {
                ciudadano.setDireccion("");
            }
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Datos cargados", "Información obtenida desde DNIC"));
        } catch (DocumentoNoEncontradoException e) {
            ciudadano = new CiudadanoDetalle();
            ciudadano.setTipoDocumento(tipo);
            ciudadano.setNumeroDocumento(numero);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Documento no se encuentra en DNIC", e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error consultando DNIC", e);
            ciudadano = new CiudadanoDetalle();
            ciudadano.setTipoDocumento(tipo);
            ciudadano.setNumeroDocumento(numero);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al consultar DNIC", "No se pudo obtener la información del ciudadano"));
        }

        docType = tipo;
        docNumber = numero;
    }

    public CiudadanoDetalle getCiudadano() {
        if (ciudadano == null) {
            ciudadano = new CiudadanoDetalle();
        }
        return ciudadano;
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
}
