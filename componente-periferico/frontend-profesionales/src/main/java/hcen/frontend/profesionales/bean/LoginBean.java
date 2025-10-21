package hcen.frontend.profesionales.bean;

import hcen.frontend.profesionales.service.APIService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private boolean loggedIn;

    @Inject
    private ProfesionalBean profesionalBean;

    @Inject
    private APIService api;


    public String login() {
        // TODO: reemplazar por auth real contra backend periférico
        if (api.loginProfesional(username, password)) {
            loggedIn = true;
            profesionalBean.cargarPerfil(username);
            // opcional: limpiar el password en memoria
            password = null;
            return "/profesional/dashboard.xhtml?faces-redirect=true";
        }

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Credenciales inválidas", "Revise usuario y contraseña"));
        return null; // permanece en login.xhtml
    }

    public String logout() {
        loggedIn = false;
        username = null;
        password = null;

        // invalidar sesión y redirigir limpio al login
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.getExternalContext().invalidateSession();
        fc.getExternalContext().getFlash().setKeepMessages(false); // no arrastrar mensajes
        // Usar navigation string para evitar IOExceptions y marcar responseComplete automáticamente
        return "/login.xhtml?faces-redirect=true";
    }

    // --- getters / setters ---
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public String getPassword() { return password; }
    public void setPassword(String p) { this.password = p; }

    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }
}
