package com.hcen.periferico.profesional.bean;

import com.hcen.periferico.profesional.dto.profesional_salud_dto;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Session Bean para profesionales de salud.
 * Mantiene la información del profesional logueado durante la sesión.
 */
@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private profesional_salud_dto profesionalLogueado;

    @PostConstruct
    public void init() {
        // Inicialización si es necesaria
    }

    public boolean isLoggedIn() {
        return profesionalLogueado != null;
    }

    /**
     * Verifica si el profesional está autenticado. Si no lo está, redirige al login.
     * Este método es usado en f:event type="preRenderView" para proteger páginas.
     */
    public void checkAuth() {
        if (!isLoggedIn()) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                String contextPath = context.getExternalContext().getRequestContextPath();
                context.getExternalContext()
                    .redirect(contextPath + "/index.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void login(profesional_salud_dto profesional) {
        this.profesionalLogueado = profesional;
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/index.xhtml?faces-redirect=true";
    }

    public String getTenantId() {
        return profesionalLogueado != null && profesionalLogueado.getTenantId() != null
            ? profesionalLogueado.getTenantId()
            : null;
    }

    public Integer getProfesionalCi() {
        return profesionalLogueado != null ? profesionalLogueado.getCi() : null;
    }

    public String getNombreProfesional() {
        return profesionalLogueado != null ? profesionalLogueado.getNombreCompleto() : "";
    }

    public String getEspecialidad() {
        return profesionalLogueado != null ? profesionalLogueado.getEspecialidad() : "";
    }

    // Getters y Setters
    public profesional_salud_dto getProfesionalLogueado() {
        return profesionalLogueado;
    }

    public void setProfesionalLogueado(profesional_salud_dto profesionalLogueado) {
        this.profesionalLogueado = profesionalLogueado;
    }
}
