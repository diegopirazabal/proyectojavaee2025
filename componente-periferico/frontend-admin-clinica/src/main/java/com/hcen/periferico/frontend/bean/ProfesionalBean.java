package com.hcen.periferico.frontend.bean;

import com.hcen.periferico.frontend.dto.especialidad_dto;
import com.hcen.periferico.frontend.dto.profesional_salud_dto;
import com.hcen.periferico.frontend.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;

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
    private profesional_salud_dto profesionalToDelete;

    private List<especialidad_dto> especialidades;  // Dropdown data
    private String searchTerm;
    private String newPasswordForEdit;  // Password opcional al editar profesional

    @PostConstruct
    public void init() {
        loadEspecialidades();
        loadProfesionales();
        newProfesional = new profesional_salud_dto();
        selectedProfesional = new profesional_salud_dto();
    }

    public void loadProfesionales() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return;
            }
            profesionales = apiService.getAllProfesionales(tenantId);
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar profesionales: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadEspecialidades() {
        try {
            especialidades = apiService.getEspecialidades();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar especialidades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void search() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return;
            }
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                loadProfesionales();
            } else {
                profesionales = apiService.searchProfesionales(searchTerm, tenantId);
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error en la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveProfesional() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessageToDialog(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                PrimeFaces.current().ajax().addCallbackParam("saveSuccess", false);
                return;
            }

            apiService.saveProfesional(
                newProfesional.getCi(),
                newProfesional.getNombre(),
                newProfesional.getApellidos(),
                newProfesional.getEspecialidadId(),
                newProfesional.getEmail(),
                newProfesional.getPassword(),
                tenantId
            );

            addMessage(FacesMessage.SEVERITY_INFO, "Profesional guardado exitosamente");
            loadProfesionales();
            newProfesional = new profesional_salud_dto(); // Reset form
            PrimeFaces.current().ajax().addCallbackParam("saveSuccess", true);
        } catch (IllegalArgumentException e) {
            addMessageToDialog(FacesMessage.SEVERITY_ERROR, e.getMessage());
            PrimeFaces.current().ajax().addCallbackParam("saveSuccess", false);
        } catch (Exception e) {
            addMessageToDialog(FacesMessage.SEVERITY_ERROR, "Error al guardar profesional: " + e.getMessage());
            PrimeFaces.current().ajax().addCallbackParam("saveSuccess", false);
            e.printStackTrace();
        }
    }

    public void updateProfesional() {
        try {
            if (selectedProfesional == null) {
                addMessageToDialog(FacesMessage.SEVERITY_ERROR, "No hay profesional seleccionado");
                PrimeFaces.current().ajax().addCallbackParam("updateSuccess", false);
                return;
            }

            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessageToDialog(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                PrimeFaces.current().ajax().addCallbackParam("updateSuccess", false);
                return;
            }

            // Si newPasswordForEdit está vacío, pasar null para no cambiar la contraseña
            String passwordToUpdate = (newPasswordForEdit != null && !newPasswordForEdit.trim().isEmpty())
                                     ? newPasswordForEdit.trim()
                                     : null;

            apiService.updateProfesional(
                selectedProfesional.getCi(),
                selectedProfesional.getNombre(),
                selectedProfesional.getApellidos(),
                selectedProfesional.getEspecialidadId(),
                selectedProfesional.getEmail(),
                passwordToUpdate,  // Password opcional: null = no cambiar
                tenantId
            );

            addMessage(FacesMessage.SEVERITY_INFO, "Profesional actualizado exitosamente");
            loadProfesionales();
            PrimeFaces.current().ajax().addCallbackParam("updateSuccess", true);
        } catch (IllegalArgumentException e) {
            addMessageToDialog(FacesMessage.SEVERITY_ERROR, e.getMessage());
            PrimeFaces.current().ajax().addCallbackParam("updateSuccess", false);
        } catch (Exception e) {
            addMessageToDialog(FacesMessage.SEVERITY_ERROR, "Error al actualizar profesional: " + e.getMessage());
            PrimeFaces.current().ajax().addCallbackParam("updateSuccess", false);
            e.printStackTrace();
        }
    }

    public void deleteProfesional(profesional_salud_dto profesional) {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return;
            }
            apiService.deleteProfesional(profesional.getCi(), tenantId);
            addMessage(FacesMessage.SEVERITY_INFO, "Profesional eliminado exitosamente");
            loadProfesionales();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al eliminar profesional: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteProfesionalSelected() {
        if (profesionalToDelete != null) {
            deleteProfesional(profesionalToDelete);
            profesionalToDelete = null;
        }
    }

    public void prepareNew() {
        newProfesional = new profesional_salud_dto();
    }

    public void prepareEdit(profesional_salud_dto profesional) {
        // Crear copia defensiva con todos los valores
        this.selectedProfesional = new profesional_salud_dto();
        this.newPasswordForEdit = null;  // Resetear password

        if (profesional != null) {
            this.selectedProfesional.setCi(profesional.getCi());
            this.selectedProfesional.setNombre(profesional.getNombre());
            this.selectedProfesional.setApellidos(profesional.getApellidos());
            this.selectedProfesional.setEspecialidadId(profesional.getEspecialidadId());
            this.selectedProfesional.setEmail(profesional.getEmail());
            this.selectedProfesional.setTenantId(profesional.getTenantId());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, message, null));
    }

    private void addMessageToDialog(FacesMessage.Severity severity, String message) {
        // Agregar mensaje asociado a un componente ficticio para que NO sea global
        // Esto permite que solo aparezca en el dialog, no en la pantalla principal
        FacesContext.getCurrentInstance().addMessage("editDialog",
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
        // SIEMPRE crear un objeto completamente nuevo para evitar estado residual de JSF
        this.selectedProfesional = new profesional_salud_dto();

        if (selectedProfesional != null) {
            this.selectedProfesional.setCi(selectedProfesional.getCi());
            this.selectedProfesional.setNombre(selectedProfesional.getNombre());
            this.selectedProfesional.setApellidos(selectedProfesional.getApellidos());
            this.selectedProfesional.setEspecialidadId(selectedProfesional.getEspecialidadId());
            this.selectedProfesional.setEmail(selectedProfesional.getEmail());
            this.selectedProfesional.setTenantId(selectedProfesional.getTenantId());
        }
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

    public String getNewPasswordForEdit() {
        return newPasswordForEdit;
    }

    public void setNewPasswordForEdit(String newPasswordForEdit) {
        this.newPasswordForEdit = newPasswordForEdit;
    }

    public profesional_salud_dto getProfesionalToDelete() {
        return profesionalToDelete;
    }

    public void setProfesionalToDelete(profesional_salud_dto profesionalToDelete) {
        this.profesionalToDelete = profesionalToDelete;
    }

    public List<especialidad_dto> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(List<especialidad_dto> especialidades) {
        this.especialidades = especialidades;
    }

    /**
     * Obtiene el nombre de una especialidad por su ID
     * Útil para mostrar en la tabla en lugar del UUID
     */
    public String getEspecialidadNombre(String especialidadId) {
        if (especialidadId == null || especialidades == null) {
            return "";
        }
        return especialidades.stream()
            .filter(e -> especialidadId.equals(e.getId()))
            .map(especialidad_dto::getNombre)
            .findFirst()
            .orElse("");
    }
}
