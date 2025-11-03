package com.hcen.core.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "PROFESIONAL_SALUD")
public class profesional_salud {

    @Id
    @Column(name = "CI")
    private Integer ci;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 80)
    private String especialidad;

    @Column(length = 150)
    private String email;

    @Column(name = "USERNAME", length = 80, unique = true)
    private String username;

    @Column(name = "PASSWORD", length = 200)
    private String password;

    // Clínicas en las que trabaja
    @ManyToMany(mappedBy = "profesionales")
    private Set<clinica> clinicas = new HashSet<>();

    // Documentos firmados
    @OneToMany(mappedBy = "profesionalFirmante")
    private List<documento_clinico> documentosFirmados = new ArrayList<>();

    public Integer getCi() { return ci; }
    public void setCi(Integer ci) { this.ci = ci; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Set<clinica> getClinicas() { return clinicas; }
    public void setClinicas(Set<clinica> clinicas) { this.clinicas = clinicas; }
    public List<documento_clinico> getDocumentosFirmados() { return documentosFirmados; }
    public void setDocumentosFirmados(List<documento_clinico> documentosFirmados) { this.documentosFirmados = documentosFirmados; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof profesional_salud p && Objects.equals(ci,p.ci)); }
    @Override public int hashCode(){ return Objects.hash(ci); }
}
