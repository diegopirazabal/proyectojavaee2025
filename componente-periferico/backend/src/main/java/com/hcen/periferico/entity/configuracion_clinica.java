package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "CONFIGURACION_CLINICA")
public class configuracion_clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "TENANT_ID", columnDefinition = "UUID", nullable = false, unique = true)
    private UUID tenantId;

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
    public configuracion_clinica() {
    }

    public configuracion_clinica(UUID tenantId) {
        this.tenantId = tenantId;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
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
        return (this == o) || (o instanceof configuracion_clinica c && Objects.equals(id, c.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
