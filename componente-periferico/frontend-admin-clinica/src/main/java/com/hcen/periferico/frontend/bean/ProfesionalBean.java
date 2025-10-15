package com.hcen.periferico.frontend.bean;

import com.hcen.core.domain.ProfesionalSalud;
import com.hcen.periferico.service.ProfesionalService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
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

    @EJB
    private ProfesionalService profesionalService;

    @Inject
    private SessionBean sessionBean;

    private List<ProfesionalSalud> profesionales;
    private ProfesionalSalud selectedProfesional;
    private ProfesionalSalud newProfesional;

    private String searchTerm;

    @PostConstruct
    public void init() {
        loadProfesionales();
        newProfesional = new ProfesionalSalud();
    }

    public void loadProfesionales() {
        try {
            profesionales = profesionalService.getAllProfesionales();
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
                profesionales = profesionalService.searchProfesionales(searchTerm);
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error en la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveProfesional() {
        try {
            profesionalService.saveProfesional(
                newProfesional.getCi(),
                newProfesional.getNombre(),
                newProfesional.getApellidos(),
                newProfesional.getEspecialidad(),
                newProfesional.getEmail()
            );

            addMessage(FacesMessage.SEVERITY_INFO, "Profesional guardado exitosamente");
            loadProfesionales();
            newProfesional = new ProfesionalSalud(); // Reset form
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

            profesionalService.saveProfesional(
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

    public void deleteProfesional(ProfesionalSalud profesional) {
        try {
            profesionalService.deleteProfesional(profesional.getCi());
            addMessage(FacesMessage.SEVERITY_INFO, "Profesional eliminado exitosamente");
            loadProfesionales();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al eliminar profesional: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void prepareNew() {
        newProfesional = new ProfesionalSalud();
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, message, null));
    }

    // Getters y Setters
    public List<ProfesionalSalud> getProfesionales() {
        return profesionales;
    }

    public void setProfesionales(List<ProfesionalSalud> profesionales) {
        this.profesionales = profesionales;
    }

    public ProfesionalSalud getSelectedProfesional() {
        return selectedProfesional;
    }

    public void setSelectedProfesional(ProfesionalSalud selectedProfesional) {
        this.selectedProfesional = selectedProfesional;
    }

    public ProfesionalSalud getNewProfesional() {
        return newProfesional;
    }

    public void setNewProfesional(ProfesionalSalud newProfesional) {
        this.newProfesional = newProfesional;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
