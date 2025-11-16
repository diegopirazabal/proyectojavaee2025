package com.hcen.periferico.frontend.dto;

import java.io.Serializable;

/**
 * DTO para transferir información de especialidades médicas.
 * Usado para comunicación entre frontend y backend (debe coincidir con backend DTO).
 */
public class especialidad_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;  // UUID as String
    private String nombre;

    // Constructores
    public especialidad_dto() {
    }

    public especialidad_dto(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return "especialidad_dto{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}
