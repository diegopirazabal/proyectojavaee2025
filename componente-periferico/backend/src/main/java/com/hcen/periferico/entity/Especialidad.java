package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Entidad que representa una especialidad médica.
 * Catálogo de especialidades disponibles para asignar a profesionales de salud.
 */
@Entity
@Table(name = "ESPECIALIDADES")
public class Especialidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "NOMBRE", length = 80, nullable = false, unique = true)
    private String nombre;

    // Constructores
    public Especialidad() {
    }

    public Especialidad(String nombre) {
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

    // toString, equals, hashCode
    @Override
    public String toString() {
        return "Especialidad{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Especialidad that = (Especialidad) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
