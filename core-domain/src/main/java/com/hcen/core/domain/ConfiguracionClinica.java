package com.hcen.core.domain;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "CONFIGURACION_CLINICA")
public class ConfiguracionClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "CLINICA_RUT", length = 20, nullable = false, unique = true)
    private String clinicaRut;

    // Personalización de Look & Feel
    @Column(name = "COLOR_PRIMARIO", length = 7)
    private String colorPrimario;

    @Column(name = "COLOR_SECUNDARIO", length = 7)
    private String colorSecundario;

    @Column(name = "LOGO_URL", length = 500)
    private String logoUrl;

    @Column(name = "NOMBRE_SISTEMA", length = 150)
    private String nombreSistema;

    @Column(name = "TEMA", length = 50)
    private String tema;

    // Configuración funcional
    @Column(name = "NODO_PERIFERICO_HABILITADO", nullable = false)
    private Boolean nodoPerifericoHabilitado = false;

    // Constructores
    public ConfiguracionClinica() {
    }

    public ConfiguracionClinica(String clinicaRut) {
        this.clinicaRut = clinicaRut;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
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

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof ConfiguracionClinica c && Objects.equals(id, c.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
