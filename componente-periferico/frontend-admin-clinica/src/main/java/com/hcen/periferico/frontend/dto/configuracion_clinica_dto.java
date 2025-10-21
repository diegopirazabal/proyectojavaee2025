package com.hcen.periferico.frontend.dto;

import java.io.Serializable;
import java.util.UUID;

public class configuracion_clinica_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String clinicaRut;
    private String colorPrimario;
    private String colorSecundario;
    private String logoUrl;
    private String nombreSistema;
    private String tema;
    private Boolean nodoPerifericoHabilitado;

    // Constructores
    public configuracion_clinica_dto() {
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClinicaRut() {
        return clinicaRut;
    }

    public void setClinicaRut(String clinicaRut) {
        this.clinicaRut = clinicaRut;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getNombreSistema() {
        return nombreSistema;
    }

    public void setNombreSistema(String nombreSistema) {
        this.nombreSistema = nombreSistema;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public Boolean getNodoPerifericoHabilitado() {
        return nodoPerifericoHabilitado;
    }

    public void setNodoPerifericoHabilitado(Boolean nodoPerifericoHabilitado) {
        this.nodoPerifericoHabilitado = nodoPerifericoHabilitado;
    }
}