package hcen.central.frontend.usuariosalud.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO para transferir datos de UsuarioSalud entre frontend y backend
 */
public class UsuarioSaludDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String cedula;
    private String tipoDocumento;
    private LocalDate fechaNacimiento;
    private String email;
    private Boolean emailVerificado;
    private String telefono;
    private String direccion;
    private String nombreCompleto;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Boolean active;
    private Boolean notificacionesHabilitadas = Boolean.TRUE;

    // Constructores
    public UsuarioSaludDTO() {}

    public UsuarioSaludDTO(String cedula, String email, String primerNombre, String primerApellido) {
        this.cedula = cedula;
        this.email = email;
        this.primerNombre = primerNombre;
        this.primerApellido = primerApellido;
    }

    // Método auxiliar para formatear fecha
    public String getFechaNacimientoFormateada() {
        if (fechaNacimiento == null) {
            return "";
        }
        return fechaNacimiento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // Getters y Setters
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

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getNotificacionesHabilitadas() {
        return notificacionesHabilitadas;
    }

    public void setNotificacionesHabilitadas(Boolean notificacionesHabilitadas) {
        this.notificacionesHabilitadas = notificacionesHabilitadas;
    }

    @Override
    public String toString() {
        return "UsuarioSaludDTO{" +
                "id=" + id +
                ", cedula='" + cedula + '\'' +
                ", primerNombre='" + primerNombre + '\'' +
                ", primerApellido='" + primerApellido + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                ", notificacionesHabilitadas=" + notificacionesHabilitadas +
                '}';
    }
}
