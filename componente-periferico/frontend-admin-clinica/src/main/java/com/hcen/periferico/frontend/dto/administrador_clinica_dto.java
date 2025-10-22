package com.hcen.periferico.frontend.dto;

import java.io.Serializable;
import java.util.UUID;

public class administrador_clinica_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String nombre;
    private String apellidos;
    private String clinica;

    // Constructores
    public administrador_clinica_dto() {
    }

    public administrador_clinica_dto(UUID id, String username, String nombre, String apellidos, String clinica) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.clinica = clinica;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}