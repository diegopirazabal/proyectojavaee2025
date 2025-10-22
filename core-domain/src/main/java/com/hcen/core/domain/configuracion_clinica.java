package com.hcen.core.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity(name = "configuracion_clinica")
@Table(name = "CONFIGURACION_CLINICA")
public class configuracion_clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "CLINICA_RUT", length = 30, nullable = false)
    private String clinicaRut;

    @Column(name = "COLOR_PRIMARIO", length = 16)
    private String colorPrimario;

    @Column(name = "COLOR_SECUNDARIO", length = 16)
    private String colorSecundario;

    @Column(name = "LOGO_URL", length = 250)
    private String logoUrl;

    @Column(name = "NOMBRE_SISTEMA", length = 120)
    private String nombreSistema;

    @Column(name = "TEMA", length = 40)
    private String tema;

    @Column(name = "NODO_PERIFERICO_HABILITADO")
    private boolean nodoPerifericoHabilitado;

    public configuracion_clinica() {}

    public configuracion_clinica(String clinicaRut) { this.clinicaRut = clinicaRut; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getClinicaRut() { return clinicaRut; }
    public void setClinicaRut(String clinicaRut) { this.clinicaRut = clinicaRut; }
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
    public boolean isNodoPerifericoHabilitado() { return nodoPerifericoHabilitado; }
    public void setNodoPerifericoHabilitado(boolean nodoPerifericoHabilitado) { this.nodoPerifericoHabilitado = nodoPerifericoHabilitado; }

    // Alias getter para compatibilidad con código que invoca getNodoPerifericoHabilitado()
    public boolean getNodoPerifericoHabilitado() { return nodoPerifericoHabilitado; }
}
