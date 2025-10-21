package hcen.frontend.admin.bean;

import hcen.frontend.admin.dto.admin_hcen_dto;
import hcen.frontend.admin.service.api_service;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Named
@SessionScoped
public class login_bean implements Serializable {

    private static final String TEMP_ADMIN_USERNAME = "admin";
    private static final String TEMP_ADMIN_PASSWORD = "admin";
    private static final String TEMP_USER_USERNAME = "user";
    private static final String TEMP_USER_PASSWORD = "user";
    private static final String TEMP_USER_DOC_TYPE = "DO";
    private static final String TEMP_USER_DOC_NUMBER = "85335898";
    
    private String username;
    private String password;
    private admin_hcen_dto loggedAdmin;
    private boolean loggedIn = false;
    
    @Inject
    private api_service apiService;
    
    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            if (isTemporaryAdminCredentials()) {
                admin_hcen_dto admin = createTemporaryAdmin();
                this.loggedAdmin = admin;
                this.loggedIn = true;

                context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Ingreso temporal",
                        "Login exitoso como administrador temporal"));

                return "admin/dashboard?faces-redirect=true";
            }

            if (isTemporaryUsuarioSaludCredentials()) {
                redirectToUsuarioSaludDashboard(context);
                return null;
            }

            admin_hcen_dto admin = apiService.authenticate(username, password);
            
            if (admin != null) {
                this.loggedAdmin = admin;
                this.loggedIn = true;
                
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Bienvenido", "Login exitoso. Bienvenido " + admin.getFullName()));
                
                return "admin/dashboard?faces-redirect=true";
            } else {
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Error de autenticación", "Usuario o contraseña incorrectos"));
                return null;
            }
        } catch (Exception e) {
            context.addMessage(null, 
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

    public String getLastLoginDisplay() {
        if (loggedAdmin == null) {
            return "";
        }
        if (loggedAdmin.getLastLogin() == null) {
            return "Primer acceso";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return loggedAdmin.getLastLogin().format(formatter);
    }

    public String getOidcLoginUrl() {
        return apiService.getOidcLoginUrl();
    }

    private boolean isTemporaryAdminCredentials() {
        return TEMP_ADMIN_USERNAME.equals(username) && TEMP_ADMIN_PASSWORD.equals(password);
    }

    private boolean isTemporaryUsuarioSaludCredentials() {
        return TEMP_USER_USERNAME.equals(username) && TEMP_USER_PASSWORD.equals(password);
    }

    private admin_hcen_dto createTemporaryAdmin() {
        admin_hcen_dto admin = new admin_hcen_dto();
        admin.setId(0L);
        admin.setUsername(TEMP_ADMIN_USERNAME);
        admin.setFirstName("Administrador");
        admin.setLastName("HCEN");
        admin.setEmail("admin@hcen.test");
        admin.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        admin.setCreatedAt(now);
        admin.setLastLogin(now);
        return admin;
    }

    private void redirectToUsuarioSaludDashboard(FacesContext context) throws IOException {
        ExternalContext externalContext = context.getExternalContext();
        String targetUrl = buildUsuarioSaludDashboardUrl(externalContext);
        externalContext.redirect(targetUrl);
    }

    private String buildUsuarioSaludDashboardUrl(ExternalContext externalContext) {
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(request.getScheme())
            .append("://")
            .append(request.getServerName());

        boolean isDefaultPort = ("http".equals(request.getScheme()) && request.getServerPort() == 80)
            || ("https".equals(request.getScheme()) && request.getServerPort() == 443);
        if (!isDefaultPort) {
            urlBuilder.append(":").append(request.getServerPort());
        }

        urlBuilder.append("/frontend-usuario-salud/dashboard.xhtml");
        urlBuilder.append("?docType=").append(encodeUrlComponent(TEMP_USER_DOC_TYPE));
        urlBuilder.append("&docNumber=").append(encodeUrlComponent(TEMP_USER_DOC_NUMBER));

        return urlBuilder.toString();
    }

    private String encodeUrlComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
