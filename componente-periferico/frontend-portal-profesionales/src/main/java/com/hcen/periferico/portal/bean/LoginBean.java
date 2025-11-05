package com.hcen.periferico.portal.bean;

import com.hcen.periferico.portal.dto.ClinicaDTO;
import com.hcen.periferico.portal.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class LoginBean {
    private String selectedTenantId; private String email; private String password;
    private List<ClinicaDTO> clinicas = new ArrayList<>();

    @Inject
    private SessionBean sessionBean;

    private final APIService api = new APIService();

    @PostConstruct
    public void init() {
        try {
            this.clinicas = api.getClinicas();
        } catch (Exception e) {
            jakarta.faces.context.FacesContext.getCurrentInstance()
                .addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Error cargando clínicas", e.getMessage()));
        }
    }

    public String login() {
        try {
            if (selectedTenantId == null || selectedTenantId.isBlank()) {
                jakarta.faces.context.FacesContext.getCurrentInstance()
                    .addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_WARN, "Aviso", "Debe seleccionar una clínica"));
                return null;
            }
            JsonObject res = api.loginProfesional(selectedTenantId, email, password);
            sessionBean.setCi(res.getInt("ci"));
            sessionBean.setNombre(res.getString("nombre", ""));
            sessionBean.setApellidos(res.getString("apellidos", ""));
            sessionBean.setTenantId(res.getString("tenantId", selectedTenantId));

            try {
                var cfg = api.getConfiguracion(sessionBean.getTenantId());
                sessionBean.setColorPrimario(cfg != null ? cfg.colorPrimario : null);
                sessionBean.setColorSecundario(cfg != null ? cfg.colorSecundario : null);
                sessionBean.setLogoUrl(cfg != null ? cfg.logoUrl : null);
                sessionBean.setNombreSistema(cfg != null ? cfg.nombreSistema : null);
                sessionBean.setTema(cfg != null ? cfg.tema : null);
            } catch (Exception ignore) {}
            return "/pages/dashboard.xhtml?faces-redirect=true";
        } catch (Exception e) {
            jakarta.faces.context.FacesContext.getCurrentInstance()
                .addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public List<ClinicaDTO> getClinicas() { return clinicas; }
    public String getSelectedTenantId(){return selectedTenantId;} public void setSelectedTenantId(String v){selectedTenantId=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
