package com.hcen.periferico.frontend.bean;

import com.hcen.periferico.frontend.dto.administrador_clinica_dto;
import com.hcen.periferico.frontend.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@RequestScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    private String username;
    private String password;
    private String clinicaRut;

    @PostConstruct
    public void init() {
        // Si ya está logueado, redirigir al dashboard
        if (sessionBean.isLoggedIn()) {
            try {
                FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .redirect("pages/dashboard.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String login() {
        try {
            // Validar campos
            if (username == null || username.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El usuario es requerido");
                return null;
            }
            if (password == null || password.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La contraseña es requerida");
                return null;
            }
            if (clinicaRut == null || clinicaRut.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El RUT de la clínica es requerido");
                return null;
            }

            // Intentar autenticar
            administrador_clinica_dto admin = apiService.authenticate(
                username.trim(),
                password,
                clinicaRut.trim()
            );

            if (admin != null) {
                // Login exitoso
                sessionBean.login(admin);
                addMessage(FacesMessage.SEVERITY_INFO, "Bienvenido " + admin.getNombreCompleto());
                return "/pages/dashboard.xhtml?faces-redirect=true";
            } else {
                // Credenciales inválidas
                addMessage(FacesMessage.SEVERITY_ERROR, "Usuario, contraseña o RUT de clínica incorrectos");
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Error en el inicio de sesión: " + e.getMessage());
            return null;
        }
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, message, null));
    }

    // Getters y Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClinicaRut() {
        return clinicaRut;
    }

    public void setClinicaRut(String clinicaRut) {
        this.clinicaRut = clinicaRut;
    }
}
