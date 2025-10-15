package com.hcen.periferico.frontend.bean;

import com.hcen.periferico.frontend.dto.profesional_salud_dto;
import com.hcen.periferico.frontend.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class ProfesionalBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    private List<profesional_salud_dto> profesionales;
    private profesional_salud_dto selectedProfesional;
    private profesional_salud_dto newProfesional;

    private String searchTerm;

    @PostConstruct
    public void init() {
        loadProfesionales();
        newProfesional = new profesional_salud_dto();
    }

    public void loadProfesionales() {
        try {
            profesionales = apiService.getAllProfesionales();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar profesionales: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void search() {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                loadProfesionales();
            } else {
                profesionales = apiService.searchProfesionales(searchTerm);
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error en la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveProfesional() {
        try {
            apiService.saveProfesional(
                newProfesional.getCi(),
                newProfesional.getNombre(),
                newProfesional.getApellidos(),
                newProfesional.getEspecialidad(),
                newProfesional.getEmail()
            );

            addMessage(FacesMessage.SEVERITY_INFO, "Profesional guardado exitosamente");
            loadProfesionales();
            newProfesional = new profesional_salud_dto(); // Reset form
        } catch (IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar profesional: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateProfesional() {
        try {
            if (selectedProfesional == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No hay profesional seleccionado");
                return;
            }

            apiService.saveProfesional(
                selectedProfesional.getCi(),
                selectedProfesional.getNombre(),
                selectedProfesional.getApellidos(),
                selectedProfesional.getEspecialidad(),
                selectedProfesional.getEmail()
            );

            addMessage(FacesMessage.SEVERITY_INFO, "Profesional actualizado exitosamente");
            loadProfesionales();
        } catch (IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al actualizar profesional: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteProfesional(profesional_salud_dto profesional) {
        try {
            apiService.deleteProfesional(profesional.getCi());
            addMessage(FacesMessage.SEVERITY_INFO, "Profesional eliminado exitosamente");
            loadProfesionales();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al eliminar profesional: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void prepareNew() {
        newProfesional = new profesional_salud_dto();
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, message, null));
    }

    // Getters y Setters
    public List<profesional_salud_dto> getProfesionales() {
        return profesionales;
    }

    public void setProfesionales(List<profesional_salud_dto> profesionales) {
        this.profesionales = profesionales;
    }

    public profesional_salud_dto getSelectedProfesional() {
        return selectedProfesional;
    }

    public void setSelectedProfesional(profesional_salud_dto selectedProfesional) {
        this.selectedProfesional = selectedProfesional;
    }

    public profesional_salud_dto getNewProfesional() {
        return newProfesional;
    }

    public void setNewProfesional(profesional_salud_dto newProfesional) {
        this.newProfesional = newProfesional;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
