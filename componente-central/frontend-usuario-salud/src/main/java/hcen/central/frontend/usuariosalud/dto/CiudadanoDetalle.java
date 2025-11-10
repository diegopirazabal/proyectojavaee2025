package hcen.central.frontend.usuariosalud.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class CiudadanoDetalle implements Serializable {

    private String tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String sexo;
    private String fechaNacimiento;
    private String codigoNacionalidad;
    private String nombreEnCedula;
    private String email = "";
    private String telefono = "";
    private String direccion = "";

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
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

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getCodigoNacionalidad() {
        return codigoNacionalidad;
    }

    public void setCodigoNacionalidad(String codigoNacionalidad) {
        this.codigoNacionalidad = codigoNacionalidad;
    }

    public String getDescripcionNacionalidad() {
        if (codigoNacionalidad == null) {
            return "";
        }
        return switch (codigoNacionalidad.trim()) {
            case "1" -> "URUGUAYA";
            case "2" -> "EXTRANJERA";
            case "3" -> "DESCONOCIDA";
            default -> codigoNacionalidad;
        };
    }

    public String getNombreEnCedula() {
        return nombreEnCedula;
    }

    public void setNombreEnCedula(String nombreEnCedula) {
        this.nombreEnCedula = nombreEnCedula;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getFechaNacimientoFormateada() {
        if (fechaNacimiento == null || fechaNacimiento.isBlank()) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(fechaNacimiento);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()));
        } catch (DateTimeParseException ignored) {
            return fechaNacimiento;
        }
    }
}
