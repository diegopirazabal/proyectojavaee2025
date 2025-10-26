package com.hcen.periferico.frontend.bean;

import com.hcen.periferico.frontend.dto.configuracion_clinica_dto;
import com.hcen.periferico.frontend.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@SessionScoped
public class ConfiguracionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    private configuracion_clinica_dto configuracion;

    // Campos temporales para edición
    private String colorPrimario;
    private String colorSecundario;
    private String logoUrl;
    private String nombreSistema;
    private String tema;
    private Boolean nodoPerifericoHabilitado;

    @PostConstruct
    public void init() {
        loadConfiguracion();
    }

    public void loadConfiguracion() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId != null) {
                configuracion = apiService.getConfiguracion(tenantId);
                if (configuracion != null) {
                    // Cargar valores en campos temporales
                    colorPrimario = configuracion.getColorPrimario();
                    colorSecundario = configuracion.getColorSecundario();
                    logoUrl = configuracion.getLogoUrl();
                    nombreSistema = configuracion.getNombreSistema();
                    tema = configuracion.getTema();
                    nodoPerifericoHabilitado = configuracion.getNodoPerifericoHabilitado();
                }
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void guardarLookAndFeel() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se puede determinar la clínica");
                return;
            }

            configuracion = apiService.updateLookAndFeel(
                tenantId,
                colorPrimario,
                colorSecundario,
                logoUrl,
                nombreSistema,
                tema
            );

            addMessage(FacesMessage.SEVERITY_INFO, "Configuración de look & feel actualizada exitosamente");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void toggleNodoPeriferico() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se puede determinar la clínica");
                return;
            }

            configuracion = apiService.toggleNodoPeriferico(
                tenantId,
                nodoPerifericoHabilitado != null ? nodoPerifericoHabilitado : false
            );

            String mensaje = nodoPerifericoHabilitado
                ? "Nodo periférico habilitado. La clínica puede conectarse al componente central."
                : "Nodo periférico deshabilitado. La clínica opera de forma aislada.";

            addMessage(FacesMessage.SEVERITY_INFO, mensaje);
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cambiar configuración de nodo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void resetearConfiguracion() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se puede determinar la clínica");
                return;
            }

            configuracion = apiService.resetConfiguracion(tenantId);
            loadConfiguracion(); // Recargar campos

            addMessage(FacesMessage.SEVERITY_INFO, "Configuración restablecida a valores por defecto");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al resetear configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, message, null));
    }

    // Getters y Setters
    public configuracion_clinica_dto getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(configuracion_clinica_dto configuracion) {
        this.configuracion = configuracion;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getNombreSistema() {
        return nombreSistema;
    }

    public void setNombreSistema(String nombreSistema) {
        this.nombreSistema = nombreSistema;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public Boolean getNodoPerifericoHabilitado() {
        return nodoPerifericoHabilitado;
    }

    public void setNodoPerifericoHabilitado(Boolean nodoPerifericoHabilitado) {
        this.nodoPerifericoHabilitado = nodoPerifericoHabilitado;
    }
}
