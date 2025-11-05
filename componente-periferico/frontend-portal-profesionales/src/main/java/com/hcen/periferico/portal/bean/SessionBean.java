package com.hcen.periferico.portal.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class SessionBean implements Serializable {
    private Integer ci; private String nombre; private String apellidos; private String tenantId;
    private String colorPrimario; private String colorSecundario; private String logoUrl; private String nombreSistema; private String tema;
    public Integer getCi(){return ci;} public void setCi(Integer v){ci=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getApellidos(){return apellidos;} public void setApellidos(String v){apellidos=v;}
    public String getTenantId(){return tenantId;} public void setTenantId(String v){tenantId=v;}
    public String getNombreCompleto(){ return (nombre!=null?nombre:"")+" "+(apellidos!=null?apellidos:"").trim(); }

    public String logout() {
        var ctx = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (ctx != null) {
            var ext = ctx.getExternalContext();
            ext.invalidateSession();
        }
        return "/index.xhtml?faces-redirect=true";
    }

    public void checkAuth() {
        var ctx = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (ctx == null) return;
        boolean logged = (tenantId != null && !tenantId.isBlank());
        if (!logged) {
            try {
                ctx.getExternalContext().redirect(ctx.getExternalContext().getRequestContextPath() + "/index.xhtml");
            } catch (Exception ignored) {}
        }
    }

    public String getColorPrimario(){ return colorPrimario; }
    public void setColorPrimario(String v){ this.colorPrimario = v; }
    public String getColorSecundario(){ return colorSecundario; }
    public void setColorSecundario(String v){ this.colorSecundario = v; }
    public String getLogoUrl(){ return logoUrl; }
    public void setLogoUrl(String v){ this.logoUrl = v; }
    public String getNombreSistema(){ return nombreSistema; }
    public void setNombreSistema(String v){ this.nombreSistema = v; }
    public String getTema(){ return tema; }
    public void setTema(String v){ this.tema = v; }
}
