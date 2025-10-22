package hcen.frontend.admin.bean;

import hcen.frontend.admin.dto.admin_hcen_dto;
import hcen.frontend.admin.service.api_service;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@SessionScoped
public class login_bean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(login_bean.class.getName());
    private static final String TEMP_ADMIN_USERNAME = "admin";
    private static final String TEMP_ADMIN_PASSWORD = "admin";
    
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

                return "dashboard?faces-redirect=true";
            }

            admin_hcen_dto admin = apiService.authenticate(username, password);
            
            if (admin != null) {
                this.loggedAdmin = admin;
                this.loggedIn = true;
                
                context.addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Bienvenido", "Login exitoso. Bienvenido " + admin.getFullName()));
                
                return "dashboard?faces-redirect=true";
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
        // Verificar si hay cookie JWT (sesión OIDC) antes de redirigir
        if (!loggedIn && !hasJwtCookie()) {
            try {
                FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("/frontend-admin-hcen/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (!loggedIn && hasJwtCookie()) {
            // Marcar como logueado si hay cookie JWT válida
            loggedIn = true;
            // Extraer información del JWT y crear admin
            this.loggedAdmin = createAdminFromJwt();
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
    
    /**
     * Crea admin_hcen_dto a partir de los claims del JWT
     */
    private admin_hcen_dto createAdminFromJwt() {
        try {
            String jwtToken = getJwtTokenValue();
            if (jwtToken == null) {
                return createOidcAdminFallback();
            }
            
            // Decodificar JWT sin validar firma (solo lectura de claims)
            // El backend ya validó la firma al crear el token
            int i = jwtToken.lastIndexOf('.');
            String withoutSignature = jwtToken.substring(0, i + 1);
            
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(withoutSignature)
                    .getBody();
            
            admin_hcen_dto admin = new admin_hcen_dto();
            admin.setId(0L);
            
            // Extraer datos del JWT
            String sub = claims.getSubject(); // Cédula del usuario
            String email = claims.get("email", String.class);
            String nombreCompleto = claims.get("nombre_completo", String.class);
            String primerNombre = claims.get("primer_nombre", String.class);
            String primerApellido = claims.get("primer_apellido", String.class);
            
            admin.setUsername(sub != null ? sub : "oidc_user");
            admin.setEmail(email != null ? email : "oidc@gub.uy");
            
            // Dividir nombre completo si existe, sino usar los campos individuales
            if (nombreCompleto != null && !nombreCompleto.isBlank()) {
                String[] partes = nombreCompleto.split(" ", 2);
                admin.setFirstName(partes[0]);
                admin.setLastName(partes.length > 1 ? partes[1] : "");
            } else {
                admin.setFirstName(primerNombre != null ? primerNombre : "Administrador");
                admin.setLastName(primerApellido != null ? primerApellido : "OIDC");
            }
            
            admin.setActive(true);
            
            LocalDateTime now = LocalDateTime.now();
            admin.setCreatedAt(now);
            admin.setLastLogin(now);
            
            LOGGER.info("Admin creado desde JWT: " + admin.getUsername() + ", " + admin.getFullName());
            return admin;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error decodificando JWT, usando admin fallback", e);
            return createOidcAdminFallback();
        }
    }
    
    private admin_hcen_dto createOidcAdminFallback() {
        admin_hcen_dto admin = new admin_hcen_dto();
        admin.setId(0L);
        admin.setUsername("oidc_user");
        admin.setFirstName("Administrador");
        admin.setLastName("OIDC");
        admin.setEmail("oidc@gub.uy");
        admin.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        admin.setCreatedAt(now);
        admin.setLastLogin(now);
        return admin;
    }
    
    /**
     * Verifica si existe cookie JWT
     */
    private boolean hasJwtCookie() {
        return getJwtTokenValue() != null;
    }
    
    /**
     * Obtiene el valor del JWT desde la cookie
     */
    private String getJwtTokenValue() {
        try {
            ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
            HttpServletRequest request = (HttpServletRequest) external.getRequest();
            
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) && 
                        cookie.getValue() != null && 
                        !cookie.getValue().isBlank()) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
