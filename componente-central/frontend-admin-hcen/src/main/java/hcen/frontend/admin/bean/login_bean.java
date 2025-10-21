package hcen.frontend.admin.bean;

import hcen.frontend.admin.dto.admin_hcen_dto;
import hcen.frontend.admin.service.api_service;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class login_bean implements Serializable {
    
    private String username;
    private String password;
    private admin_hcen_dto loggedAdmin;
    private boolean loggedIn = false;
    
    @Inject
    private api_service apiService;
    
    public String login() {
        try {
            admin_hcen_dto admin = apiService.authenticate(username, password);
            
            if (admin != null) {
                this.loggedAdmin = admin;
                this.loggedIn = true;
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Bienvenido", "Login exitoso. Bienvenido " + admin.getFullName()));
                
                return "admin/dashboard?faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Error de autenticación", "Usuario o contraseña incorrectos"));
                return null;
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error del sistema", "Error al conectar con el servidor"));
            return null;
        }
    }
    
    public String logout() {
        this.loggedAdmin = null;
        this.loggedIn = false;
        this.username = null;
        this.password = null;
        
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sesión cerrada", "Has cerrado sesión exitosamente"));
        
        return "/login?faces-redirect=true";
    }
    
    public void checkAuthentication() {
        if (!loggedIn) {
            try {
                FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("/frontend-admin-hcen/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Getters and Setters
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
    
    public admin_hcen_dto getLoggedAdmin() {
        return loggedAdmin;
    }
    
    public void setLoggedAdmin(admin_hcen_dto loggedAdmin) {
        this.loggedAdmin = loggedAdmin;
    }
    
    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}