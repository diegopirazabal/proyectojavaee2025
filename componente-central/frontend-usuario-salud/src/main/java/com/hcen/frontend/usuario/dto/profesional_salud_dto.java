package com.hcen.frontend.usuario.dto;

import java.io.Serializable;

public class profesional_salud_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer ci;
    private String nombre;
    private String apellidos;
    private String especialidad;
    private String email;

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

    public String getNombreCompleto() { return (nombre != null ? nombre : "") + " " + (apellidos != null ? apellidos : "").trim(); }
}
