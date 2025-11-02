package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "USUARIO_SALUD")
public class usuario_salud {

    @Id
    @Column(name = "cedula", length = 20)
    private String cedula;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;

    @Column(name = "primer_nombre", length = 100)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;

    @Column(name = "primer_apellido", length = 100)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;

    @Column(length = 150)
    private String email;

    @Column(name = "fecha_nac")
    private LocalDate fechaNac;

    @Column(length = 200)
    private String direccion;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "sincronizado_central")
    private Boolean sincronizadoCentral;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Campos legacy para compatibilidad (deprecated)
    @Column(name = "ci")
    private Integer ci;

    @Column(length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(name = "fechanac")
    private LocalDate fechanac;

    // RELACIONES /.//

    // Historia Clínica
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private historia_clinica historiaClinica;

    // Notificaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<notificacion> notificaciones = new ArrayList<>();

    // Clínicas
    @ManyToMany
    @JoinTable(name = "USUARIO_CLINICA",
            joinColumns = @JoinColumn(name = "USUARIO_CI"),
            inverseJoinColumns = @JoinColumn(name = "CLINICA_ID"))
    private Set<clinica> clinicas = new HashSet<>();

    // Políticas de acceso
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<politica_acceso> politicasAcceso = new ArrayList<>();

    // GETTERS Y SETTERS

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getFechaNac() { return fechaNac; }
    public void setFechaNac(LocalDate fechaNac) { this.fechaNac = fechaNac; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getSincronizadoCentral() { return sincronizadoCentral; }
    public void setSincronizadoCentral(Boolean sincronizadoCentral) { this.sincronizadoCentral = sincronizadoCentral; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Legacy getters/setters (deprecated)
    public Integer getCi() { return ci; }
    public void setCi(Integer ci) { this.ci = ci; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechanac() { return fechanac; }
    public void setFechanac(LocalDate fechanac) { this.fechanac = fechanac; }
    public historia_clinica getHistoriaClinica() { return historiaClinica; }
    public void setHistoriaClinica(historia_clinica historiaClinica) {
        this.historiaClinica = historiaClinica;
        if (historiaClinica != null) historiaClinica.setUsuario(this);
    }
    public List<notificacion> getNotificaciones() { return notificaciones; }
    public void setNotificaciones(List<notificacion> notificaciones) { this.notificaciones = notificaciones; }
    public Set<clinica> getClinicas() { return clinicas; }
    public void setClinicas(Set<clinica> clinicas) { this.clinicas = clinicas; }
    public List<politica_acceso> getPoliticasAcceso() { return politicasAcceso; }
    public void setPoliticasAcceso(List<politica_acceso> politicasAcceso) { this.politicasAcceso = politicasAcceso; }

    // equals/hashCode por PK
    @Override public boolean equals(Object o){ return (this==o) || (o instanceof usuario_salud u && Objects.equals(cedula,u.cedula)); }
    @Override public int hashCode(){ return Objects.hash(cedula); }
}
