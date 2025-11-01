package hcen.frontend.admin.bean;

import hcen.frontend.admin.dto.clinica_dto;
import hcen.frontend.admin.dto.clinica_form;
import hcen.frontend.admin.dto.prestador_dto;
import hcen.frontend.admin.dto.prestador_form;
import hcen.frontend.admin.dto.usuario_salud_dto;
import hcen.frontend.admin.service.api_service;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.context.ExternalContext;
import jakarta.servlet.http.HttpSession;

@Named
@ViewScoped
public class dashboard_bean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(dashboard_bean.class.getName());

    @Inject
    private api_service apiService;

    private List<usuario_salud_dto> usuariosSalud = new ArrayList<>();
    private List<prestador_dto> prestadores = new ArrayList<>();
    private prestador_form nuevoPrestador = new prestador_form();
    private List<clinica_dto> clinicas = new ArrayList<>();
    private clinica_form nuevaClinica = new clinica_form();

    @PostConstruct
    public void init() {
        mostrarAdvertenciaMenorEdad();
        cargarUsuariosSalud();
        cargarPrestadores();
        cargarClinicas();
    }

    public void cargarUsuariosSalud() {
        try {
            usuariosSalud = apiService.obtenerUsuariosSalud();
        } catch (Exception e) {
            usuariosSalud = new ArrayList<>();
            addErrorMessage("Error al cargar usuarios de salud", e.getMessage());
        }
    }

    public void cargarPrestadores() {
        try {
            prestadores = apiService.obtenerPrestadores();
        } catch (Exception e) {
            prestadores = new ArrayList<>();
            addErrorMessage("Error al cargar prestadores", e.getMessage());
        }
    }

    public void cargarClinicas() {
        try {
            clinicas = apiService.obtenerClinicas();
        } catch (Exception e) {
            clinicas = new ArrayList<>();
            addErrorMessage("Error al cargar clínicas", e.getMessage());
        }
    }

    public void enviarNotificacion(usuario_salud_dto usuario) {
        if (usuario == null || usuario.getCedula() == null) {
            addErrorMessage("Notificación no enviada", "El usuario seleccionado no es válido.");
            return;
        }
        boolean success = apiService.enviarNotificacionUsuario(usuario.getCedula(), "mensaje de prueba");
        if (success) {
            addInfoMessage("Notificación enviada", "Se notificó a " + usuario.getDisplayName());
        } else {
            addErrorMessage("Notificación no enviada", "No se pudo contactar al backend para el usuario " + usuario.getCedula());
        }
    }

    public void crearPrestador() {
        String nombrePrestador = nuevoPrestador.getNombre();
        String resultado = apiService.crearPrestador(nuevoPrestador);
        boolean exito = resultado == null;
        PrimeFaces.current().ajax().addCallbackParam("prestadorCreado", exito);
        if (exito) {
            addInfoMessage("Prestador creado", "Se creó el prestador " + nombrePrestador);
            nuevoPrestador.reset();
            cargarPrestadores();
        } else {
            addErrorMessage("Alta de prestador", resultado);
        }
    }

    public void crearClinica() {
        String nombreClinica = nuevaClinica.getNombre();
        String resultado = apiService.crearClinica(nuevaClinica);
        boolean exito = resultado == null;
        PrimeFaces.current().ajax().addCallbackParam("clinicaCreada", exito);
        if (exito) {
            addInfoMessage("Clínica creada", "Se creó la clínica " + nombreClinica);
            nuevaClinica.reset();
            cargarClinicas();
        } else {
            addErrorMessage("Alta de clínica", resultado);
        }
    }

    public List<usuario_salud_dto> getUsuariosSalud() {
        return usuariosSalud;
    }

    public List<prestador_dto> getPrestadores() {
        return prestadores;
    }

    public prestador_form getNuevoPrestador() {
        return nuevoPrestador;
    }

    public List<clinica_dto> getClinicas() {
        return clinicas;
    }

    public clinica_form getNuevaClinica() {
        return nuevaClinica;
    }

    private void addInfoMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    private void mostrarAdvertenciaMenorEdad() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);
        if (session == null) {
            return;
        }
        Object jwtTokenObj = session.getAttribute("jwtToken");
        if (jwtTokenObj == null) {
            return;
        }
        try {
            Object warning = jwtTokenObj.getClass().getMethod("getWarningMessage").invoke(jwtTokenObj);
            if (warning instanceof String warningText && !warningText.isBlank()) {
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", warningText));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo obtener advertencia del JWT", e);
        }
    }
}
