package com.hcen.periferico.profesional.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

@Named
@RequestScoped
public class DocumentosBean {
    private String pacienteCedula;
    private String tipo;
    private String titulo;
    private String contenido;

    public String guardar() {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Documento", "Guardado (pendiente de integración backend)"));
        return "/pages/dashboard.xhtml?faces-redirect=true";
    }

    public String cancelar() { return "/pages/dashboard.xhtml?faces-redirect=true"; }

    public String getPacienteCedula() { return pacienteCedula; }
    public void setPacienteCedula(String pacienteCedula) { this.pacienteCedula = pacienteCedula; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}

