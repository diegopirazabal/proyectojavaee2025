package com.hcen.core.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity(name = "administrador_clinica")
@Table(name = "ADMINISTRADOR_CLINICA")
public class administrador_clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "USERNAME", length = 80, nullable = false)
    private String username;

    @Column(name = "PASSWORD", length = 120, nullable = false)
    private String password;

    @Column(name = "NOMBRE", length = 100)
    private String nombre;

    @Column(name = "APELLIDOS", length = 120)
    private String apellidos;

    @Column(name = "CLINICA", length = 30, nullable = false)
    private String clinica; // RUT de clínica

    public administrador_clinica() {}

    public administrador_clinica(String username, String password, String nombre, String apellidos, String clinica) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.clinica = clinica;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getClinica() { return clinica; }
    public void setClinica(String clinica) { this.clinica = clinica; }
}

