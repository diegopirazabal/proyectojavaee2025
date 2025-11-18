package com.hcen.periferico.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO para transferir información de especialidades médicas.
 * Usado para comunicación entre backend y frontend.
 */
public class especialidad_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nombre;

    // Constructores
    public especialidad_dto() {
    }

    public especialidad_dto(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}
