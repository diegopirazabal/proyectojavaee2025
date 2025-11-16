package com.hcen.periferico.profesional.dto;

import java.io.Serializable;

public class profesional_salud_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer ci;
    private String nombre;
    private String apellidos;
    private String especialidad;
    private String email;
    private String tenantId;
    private Boolean active;

    // Constructores
    public profesional_salud_dto() {
    }

    public profesional_salud_dto(Integer ci, String nombre, String apellidos, String especialidad, String email) {
        this.ci = ci;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.especialidad = especialidad;
        this.email = email;
    }

    // Getters y Setters
    public Integer getCi() {
        return ci;
    }

    public void setCi(Integer ci) {
        this.ci = ci;
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

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}
