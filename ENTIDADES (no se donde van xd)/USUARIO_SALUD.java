package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "USUARIO_SALUD")
public class UsuarioSalud {

    @Id
    @Column(name = "CI")
    private Integer ci;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    private LocalDate fechaNac;

    @Column(length = 150)
    private String email;

    @Column(length = 200)
    private String direccion;

    // RELACIONES /.//

    // Historia Clínica
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private HistoriaClinica historiaClinica;

    // Notificaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notificacion> notificaciones = new ArrayList<>();

    // Clínicas
    @ManyToMany
    @JoinTable(name = "USUARIO_CLINICA",
            joinColumns = @JoinColumn(name = "USUARIO_CI"),
            inverseJoinColumns = @JoinColumn(name = "CLINICA_ID"))
    private Set<Clinica> clinicas = new HashSet<>();

    // Políticas de acceso
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PoliticaAcceso> politicasAcceso = new ArrayList<>();

    // GETTERS Y SETTERS

    public Integer getCi() { return ci; }
    public void setCi(Integer ci) { this.ci = ci; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNac() { return fechaNac; }
    public void setFechaNac(LocalDate fechaNac) { this.fechaNac = fechaNac; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public HistoriaClinica getHistoriaClinica() { return historiaClinica; }
    public void setHistoriaClinica(HistoriaClinica historiaClinica) {
        this.historiaClinica = historiaClinica;
        if (historiaClinica != null) historiaClinica.setUsuario(this);
    }
    public List<Notificacion> getNotificaciones() { return notificaciones; }
    public void setNotificaciones(List<Notificacion> notificaciones) { this.notificaciones = notificaciones; }
    public Set<Clinica> getClinicas() { return clinicas; }
    public void setClinicas(Set<Clinica> clinicas) { this.clinicas = clinicas; }
    public List<PoliticaAcceso> getPoliticasAcceso() { return politicasAcceso; }
    public void setPoliticasAcceso(List<PoliticaAcceso> politicasAcceso) { this.politicasAcceso = politicasAcceso; }

    // equals/hashCode por PK
    @Override public boolean equals(Object o){ return (this==o) || (o instanceof UsuarioSalud u && Objects.equals(ci,u.ci)); }
    @Override public int hashCode(){ return Objects.hash(ci); }
}
