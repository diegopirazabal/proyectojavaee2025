package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ADMINISTRADOR_CLINICA",
       uniqueConstraints = @UniqueConstraint(columnNames = {"USERNAME", "TENANT_ID"}))
public class administrador_clinica {

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

    @Column(name = "TENANT_ID", columnDefinition = "UUID", nullable = false)
    private UUID tenantId;

    // Constructores
    public administrador_clinica() {
    }

    public administrador_clinica(String username, String password, String nombre,
                               String apellidos, UUID tenantId) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.tenantId = tenantId;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof administrador_clinica a && Objects.equals(id, a.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
