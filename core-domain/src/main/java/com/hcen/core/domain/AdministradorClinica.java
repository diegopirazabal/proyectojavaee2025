package com.hcen.core.domain;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ADMINISTRADOR_CLINICA",
       uniqueConstraints = @UniqueConstraint(columnNames = {"USERNAME", "CLINICA_RUT"}))
public class AdministradorClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "USERNAME", length = 80, nullable = false)
    private String username;

    @Column(name = "PASSWORD", length = 200, nullable = false)
    private String password;

    @Column(name = "NOMBRE", length = 100, nullable = false)
    private String nombre;

    @Column(name = "APELLIDOS", length = 100, nullable = false)
    private String apellidos;

    @Column(name = "CLINICA_RUT", length = 20, nullable = false)
    private String clinica;

    // Constructores
    public AdministradorClinica() {
    }

    public AdministradorClinica(String username, String password, String nombre,
                               String apellidos, String clinica) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.clinica = clinica;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getClinica() {
        return clinica;
    }

    public void setClinica(String clinica) {
        this.clinica = clinica;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof AdministradorClinica a && Objects.equals(id, a.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
