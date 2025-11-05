package com.hcen.periferico.profesional.bean;

import com.hcen.periferico.profesional.dto.clinica_dto;
import com.hcen.periferico.profesional.dto.profesional_salud_dto;
import com.hcen.periferico.profesional.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    private profesional_salud_dto profesionalLogueado;
    private List<clinica_dto> clinicas;

    private String selectedTenantId;
    private String email;
    private String password;

    @PostConstruct
    public void init() {
        clinicas = new ArrayList<>();
        loadClinicas();
    }

    public void loadClinicas() {
        try {
            clinicas = apiService.getClinicas();
            if (clinicas.size() == 1) {
                selectedTenantId = clinicas.get(0).getTenantId();
            } else if (clinicas.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_WARN, "No hay clínicas registradas para seleccionar.");
            }
        } catch (Exception e) {
            clinicas = new ArrayList<>();
            addMessage(FacesMessage.SEVERITY_ERROR,
                "No se pudieron obtener las clínicas disponibles: " + e.getMessage());
        }
    }

    public boolean isLoggedIn() {
        return profesionalLogueado != null;
    }

    public void checkAuth() {
        if (!isLoggedIn()) {
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                String contextPath = context.getExternalContext().getRequestContextPath();
                context.getExternalContext().redirect(contextPath + "/index.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String navigateToDocumentos() {
        if (!isLoggedIn()) {
            if (clinicas == null || clinicas.isEmpty()) {
                loadClinicas();
            }
            if (selectedTenantId == null || selectedTenantId.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Debe seleccionar una clínica.");
                return null;
            }
            selectedTenantId = selectedTenantId.trim();
            if (email == null || email.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Debe ingresar el correo electrónico.");
                return null;
            }
            if (password == null || password.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Debe ingresar la contraseña.");
                return null;
            }

            try {
                String normalizedEmail = email.trim();
                profesional_salud_dto profesional = apiService.loginProfesional(
                    selectedTenantId,
                    normalizedEmail,
                    password
                );

                if (profesional == null) {
                    addMessage(FacesMessage.SEVERITY_ERROR,
                        "Credenciales inválidas o profesional no asociado a la clínica seleccionada.");
                    return null;
                }

                if (profesional.getTenantId() == null) {
                    profesional.setTenantId(selectedTenantId);
                }

                login(profesional);
                email = normalizedEmail;
                password = null;
            } catch (IOException e) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                    "No se pudo comunicar con el servidor: " + e.getMessage());
                return null;
            }
        }
        return "/pages/documentos.xhtml?faces-redirect=true";
    }

    public void login(profesional_salud_dto profesional) {
        this.profesionalLogueado = profesional;
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/index.xhtml?faces-redirect=true";
    }

    public String getTenantId() {
        return profesionalLogueado != null ? profesionalLogueado.getTenantId() : null;
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

    public profesional_salud_dto getProfesionalLogueado() {
        return profesionalLogueado;
    }

    public void setProfesionalLogueado(profesional_salud_dto profesionalLogueado) {
        this.profesionalLogueado = profesionalLogueado;
    }

    public List<clinica_dto> getClinicas() {
        return clinicas;
    }

    public String getSelectedTenantId() {
        return selectedTenantId;
    }

    public void setSelectedTenantId(String selectedTenantId) {
        this.selectedTenantId = selectedTenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, null));
    }
}
