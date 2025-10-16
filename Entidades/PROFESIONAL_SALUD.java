package com.hcen.core.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "PROFESIONAL_SALUD")
public class ProfesionalSalud {

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

    // Clínicas en las que trabaja
    @ManyToMany(mappedBy = "profesionales")
    private Set<Clinica> clinicas = new HashSet<>();

    // Documentos firmados
    @OneToMany(mappedBy = "profesionalFirmante")
    private List<DocumentoClinico> documentosFirmados = new ArrayList<>();

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
    public Set<Clinica> getClinicas() { return clinicas; }
    public void setClinicas(Set<Clinica> clinicas) { this.clinicas = clinicas; }
    public List<DocumentoClinico> getDocumentosFirmados() { return documentosFirmados; }
    public void setDocumentosFirmados(List<DocumentoClinico> documentosFirmados) { this.documentosFirmados = documentosFirmados; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof ProfesionalSalud p && Objects.equals(ci,p.ci)); }
    @Override public int hashCode(){ return Objects.hash(ci); }
}
