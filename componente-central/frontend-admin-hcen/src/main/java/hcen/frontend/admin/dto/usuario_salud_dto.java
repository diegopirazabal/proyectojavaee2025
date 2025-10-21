package hcen.frontend.admin.dto;

import java.io.Serializable;
import java.time.Instant;

public class usuario_salud_dto implements Serializable {

    private Long id;
    private String cedula;
    private String nombreCompleto;
    private String primerNombre;
    private String primerApellido;
    private String email;
    private Boolean active;
    private Instant createdAt;
    private Instant lastLogin;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getDisplayName() {
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            return nombreCompleto;
        }
        String first = primerNombre != null ? primerNombre : "";
        String last = primerApellido != null ? primerApellido : "";
        return (first + " " + last).trim();
    }
}
