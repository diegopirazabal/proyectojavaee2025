package com.hcen.frontend.usuario.bean;

import com.hcen.frontend.usuario.dto.administrador_clinica_dto;
import com.hcen.frontend.usuario.service.api_service;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;

@Named
@SessionScoped
public class login_bean implements Serializable {

    private String username;
    private String password;
    private String cedula;
    private administrador_clinica_dto usuario;
    private boolean loggedIn = false;

    @Inject
    private api_service apiService;

    public String login() {
        try {
            administrador_clinica_dto dto = apiService.authenticate(username, password);
            if (dto != null) {
                this.usuario = dto;
                this.loggedIn = true;
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Bienvenido", "Login exitoso. Hola " + (dto.getNombreCompleto())));
                return "usuario/dashboard?faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error de autenticación", "Credenciales inválidas o clínica incorrecta"));
                return null;
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error del sistema", "No se pudo conectar con el servidor"));
            return null;
        }
    }

    public String ingresarPorCedula() {
        if (cedula == null || cedula.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Dato requerido", "Ingrese su cédula"));
            return null;
        }
        this.loggedIn = true;
        return "usuario/historia?faces-redirect=true";
    }

    public String logout() {
        this.usuario = null;
        this.loggedIn = false;
        this.username = null;
        this.password = null;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }

    public void checkAuthentication() {
        if (!loggedIn) {
            try {
                var ec = FacesContext.getCurrentInstance().getExternalContext();
                ec.redirect(ec.getRequestContextPath() + "/login.xhtml");
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // Getters / setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public administrador_clinica_dto getUsuario() { return usuario; }
    public void setUsuario(administrador_clinica_dto usuario) { this.usuario = usuario; }
    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }
}
