package hcen.central.inus.dto;

import hcen.central.inus.entity.UsuarioSalud;

import java.time.Instant;

/**
 * DTO para exponer información resumida de los usuarios de salud
 * hacia el frontend administrativo.
 */
public class UsuarioSaludResponse {

    private Long id;
    private String cedula;
    private String nombreCompleto;
    private String primerNombre;
    private String primerApellido;
    private String email;
    private Boolean active;
    private Instant createdAt;
    private Instant lastLogin;

    public static UsuarioSaludResponse fromEntity(UsuarioSalud entity) {
        UsuarioSaludResponse dto = new UsuarioSaludResponse();
        dto.setId(entity.getId());
        dto.setCedula(entity.getCedula());
        dto.setNombreCompleto(resolveNombreCompleto(entity));
        dto.setPrimerNombre(entity.getPrimerNombre());
        dto.setPrimerApellido(entity.getPrimerApellido());
        dto.setEmail(entity.getEmail());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastLogin(entity.getLastLogin());
        return dto;
    }

    private static String resolveNombreCompleto(UsuarioSalud entity) {
        if (entity.getNombreCompleto() != null && !entity.getNombreCompleto().isBlank()) {
            return entity.getNombreCompleto();
        }
        StringBuilder builder = new StringBuilder();
        if (entity.getPrimerNombre() != null) {
            builder.append(entity.getPrimerNombre());
        }
        if (entity.getSegundoNombre() != null && !entity.getSegundoNombre().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(entity.getSegundoNombre());
        }
        if (entity.getPrimerApellido() != null) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(entity.getPrimerApellido());
        }
        if (entity.getSegundoApellido() != null && !entity.getSegundoApellido().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(entity.getSegundoApellido());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

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
}
