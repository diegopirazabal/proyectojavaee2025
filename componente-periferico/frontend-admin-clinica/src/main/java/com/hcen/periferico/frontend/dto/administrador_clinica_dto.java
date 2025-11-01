package com.hcen.periferico.frontend.dto;

import java.io.Serializable;
import java.util.UUID;

public class administrador_clinica_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String nombre;
    private String apellidos;
    private UUID tenantId;

    // Constructores
    public administrador_clinica_dto() {
    }

    public administrador_clinica_dto(UUID id, String username, String nombre, String apellidos, UUID tenantId) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.tenantId = tenantId;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}