package com.hcen.periferico.dto;

import java.io.Serializable;

public class profesional_salud_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer ci;
    private String nombre;
    private String apellidos;
    private String especialidadId;  // UUID as String
    private String email;
    private String password;
    private String tenantId;
    private Boolean active;

    // Constructores
    public profesional_salud_dto() {
    }

    public profesional_salud_dto(Integer ci, String nombre, String apellidos, String especialidadId, String email) {
        this.ci = ci;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.especialidadId = especialidadId;
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

    public String getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(String especialidadId) {
        this.especialidadId = especialidadId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
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
}
