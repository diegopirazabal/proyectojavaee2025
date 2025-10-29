package hcen.frontend.admin.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class clinica_dto implements Serializable {

    private UUID tenantId;
    private String nombre;
    private String direccion;
    private String email;
    private String estado;
    private LocalDateTime fecRegistro;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFecRegistro() { return fecRegistro; }
    public void setFecRegistro(LocalDateTime fecRegistro) { this.fecRegistro = fecRegistro; }

    public String getFecRegistroFormatted() {
        if (fecRegistro == null) {
            return "-";
        }
        return fecRegistro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
