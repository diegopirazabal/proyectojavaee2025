package com.hcen.periferico.profesional.dto;

import java.io.Serializable;

public class configuracion_clinica_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String colorPrimario;
    private String colorSecundario;
    private String logoUrl;
    private String nombreSistema;
    private String tema;
    private Boolean nodoPerifericoHabilitado;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getColorPrimario() { return colorPrimario; }
    public void setColorPrimario(String colorPrimario) { this.colorPrimario = colorPrimario; }

    public String getColorSecundario() { return colorSecundario; }
    public void setColorSecundario(String colorSecundario) { this.colorSecundario = colorSecundario; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getNombreSistema() { return nombreSistema; }
    public void setNombreSistema(String nombreSistema) { this.nombreSistema = nombreSistema; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public Boolean getNodoPerifericoHabilitado() { return nodoPerifericoHabilitado; }
    public void setNodoPerifericoHabilitado(Boolean nodoPerifericoHabilitado) { this.nodoPerifericoHabilitado = nodoPerifericoHabilitado; }
}

