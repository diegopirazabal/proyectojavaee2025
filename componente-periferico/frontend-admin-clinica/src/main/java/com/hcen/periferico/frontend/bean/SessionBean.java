package com.hcen.periferico.frontend.bean;

import com.hcen.core.domain.AdministradorClinica;
import com.hcen.core.domain.ConfiguracionClinica;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private AdministradorClinica administradorLogueado;
    private ConfiguracionClinica configuracion;

    @PostConstruct
    public void init() {
        // Inicialización si es necesaria
    }

    public boolean isLoggedIn() {
        return administradorLogueado != null;
    }

    public void login(AdministradorClinica admin) {
        this.administradorLogueado = admin;
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/index.xhtml?faces-redirect=true";
    }

    public String getClinicaRut() {
        return administradorLogueado != null ? administradorLogueado.getClinica() : null;
    }

    public String getNombreAdministrador() {
        return administradorLogueado != null ? administradorLogueado.getNombreCompleto() : "";
    }

    public String getUsername() {
        return administradorLogueado != null ? administradorLogueado.getUsername() : "";
    }

    // Getters y Setters
    public AdministradorClinica getAdministradorLogueado() {
        return administradorLogueado;
    }

    public void setAdministradorLogueado(AdministradorClinica administradorLogueado) {
        this.administradorLogueado = administradorLogueado;
    }

    public ConfiguracionClinica getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionClinica configuracion) {
        this.configuracion = configuracion;
    }
}
