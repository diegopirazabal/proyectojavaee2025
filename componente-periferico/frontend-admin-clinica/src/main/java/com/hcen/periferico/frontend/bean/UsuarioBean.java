package com.hcen.periferico.frontend.bean;

import com.hcen.periferico.frontend.dto.usuario_salud_dto;
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
public class UsuarioBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    private List<usuario_salud_dto> usuarios;
    private usuario_salud_dto selectedUsuario;
    private usuario_salud_dto newUsuario;
    private usuario_salud_dto usuarioToDelete;

    private String searchTerm;

    @PostConstruct
    public void init() {
        loadUsuarios();
        newUsuario = new usuario_salud_dto();
        selectedUsuario = new usuario_salud_dto();
    }

    public void loadUsuarios() {
        try {
            usuarios = apiService.getAllUsuarios();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void search() {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                loadUsuarios();
            } else {
                usuarios = apiService.searchUsuarios(searchTerm);
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error en la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void registrarUsuario() {
        try {
            // Obtener el RUT de la clínica desde la sesión
            String clinicaRut = sessionBean.getClinicaRut();
            if (clinicaRut == null || clinicaRut.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se puede determinar la clínica. Por favor, recargue la sesión.");
                return;
            }

            // Validaciones básicas
            if (newUsuario.getCedula() == null || newUsuario.getCedula().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La cédula es requerida");
                return;
            }
            if (newUsuario.getTipoDocumento() == null || newUsuario.getTipoDocumento().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El tipo de documento es requerido");
                return;
            }
            if (newUsuario.getPrimerNombre() == null || newUsuario.getPrimerNombre().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El primer nombre es requerido");
                return;
            }
            if (newUsuario.getPrimerApellido() == null || newUsuario.getPrimerApellido().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El primer apellido es requerido");
                return;
            }
            if (newUsuario.getEmail() == null || newUsuario.getEmail().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El email es requerido");
                return;
            }
            if (newUsuario.getFechaNacimiento() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La fecha de nacimiento es requerida");
                return;
            }

            // Llamar al servicio para registrar
            usuario_salud_dto resultado = apiService.registrarUsuario(
                newUsuario.getCedula(),
                newUsuario.getTipoDocumento(),
                newUsuario.getPrimerNombre(),
                newUsuario.getSegundoNombre(),
                newUsuario.getPrimerApellido(),
                newUsuario.getSegundoApellido(),
                newUsuario.getEmail(),
                newUsuario.getFechaNacimiento(),
                clinicaRut
            );

            if (resultado != null) {
                addMessage(FacesMessage.SEVERITY_INFO,
                    "Usuario registrado exitosamente en el sistema central y asociado a la clínica");
                loadUsuarios();
                newUsuario = new usuario_salud_dto(); // Reset form
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo registrar el usuario");
            }

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteUsuario(usuario_salud_dto usuario) {
        try {
            // Obtener el RUT de la clínica desde la sesión
            String clinicaRut = sessionBean.getClinicaRut();
            if (clinicaRut == null || clinicaRut.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se puede determinar la clínica. Por favor, recargue la sesión.");
                return;
            }

            boolean success = apiService.deleteUsuario(usuario.getCedula(), clinicaRut);
            if (success) {
                addMessage(FacesMessage.SEVERITY_INFO, "Usuario desasociado de la clínica exitosamente");
                loadUsuarios();
            } else {
                addMessage(FacesMessage.SEVERITY_WARN, "No se pudo desasociar el usuario de la clínica");
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al desasociar usuario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteUsuarioSelected() {
        if (usuarioToDelete != null) {
            deleteUsuario(usuarioToDelete);
            usuarioToDelete = null;
        }
    }

    public void prepareNew() {
        newUsuario = new usuario_salud_dto();
        // Setear CI como tipo de documento por defecto
        newUsuario.setTipoDocumento("CI");
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, message, null));
    }

    // Getters y Setters
    public List<usuario_salud_dto> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<usuario_salud_dto> usuarios) {
        this.usuarios = usuarios;
    }

    public usuario_salud_dto getSelectedUsuario() {
        return selectedUsuario;
    }

    public void setSelectedUsuario(usuario_salud_dto selectedUsuario) {
        this.selectedUsuario = selectedUsuario;
    }

    public usuario_salud_dto getNewUsuario() {
        return newUsuario;
    }

    public void setNewUsuario(usuario_salud_dto newUsuario) {
        this.newUsuario = newUsuario;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public usuario_salud_dto getUsuarioToDelete() {
        return usuarioToDelete;
    }

    public void setUsuarioToDelete(usuario_salud_dto usuarioToDelete) {
        this.usuarioToDelete = usuarioToDelete;
    }
}
