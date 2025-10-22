package com.hcen.periferico.frontend.bean;

import com.hcen.periferico.frontend.dto.administrador_clinica_dto;
import com.hcen.periferico.frontend.dto.configuracion_clinica_dto;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private administrador_clinica_dto administradorLogueado;
    private configuracion_clinica_dto configuracion;

    @PostConstruct
    public void init() {
        // Inicialización si es necesaria
    }

    public boolean isLoggedIn() {
        return administradorLogueado != null;
    }

    /**
     * Verifica si el usuario está autenticado. Si no lo está, redirige al login.
     * Este método es usado en f:event type="preRenderView" para proteger páginas.
     */
    public void checkAuth() {
        if (!isLoggedIn()) {
            try {
                FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .redirect("index.xhtml");
            } catch (Exception e) {
                // Log error si es necesario
                e.printStackTrace();
            }
        }
    }

    public void login(administrador_clinica_dto admin) {
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
    public administrador_clinica_dto getAdministradorLogueado() {
        return administradorLogueado;
    }

    public void setAdministradorLogueado(administrador_clinica_dto administradorLogueado) {
        this.administradorLogueado = administradorLogueado;
    }

    public configuracion_clinica_dto getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(configuracion_clinica_dto configuracion) {
        this.configuracion = configuracion;
    }
}
