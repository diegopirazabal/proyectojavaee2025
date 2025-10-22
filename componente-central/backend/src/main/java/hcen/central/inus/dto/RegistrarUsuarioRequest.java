package hcen.central.inus.dto;

import hcen.central.inus.enums.TipoDocumento;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para solicitud de registro de usuario en una clínica
 */
public class RegistrarUsuarioRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private TipoDocumento tipoDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    private LocalDate fechaNacimiento;
    private String clinicaRut;

    // Constructores
    public RegistrarUsuarioRequest() {}

    public RegistrarUsuarioRequest(String cedula, TipoDocumento tipoDocumento,
                                   String primerNombre, String primerApellido,
                                   String email, LocalDate fechaNacimiento, String clinicaRut) {
        this.cedula = cedula;
        this.tipoDocumento = tipoDocumento;
        this.primerNombre = primerNombre;
        this.primerApellido = primerApellido;
        this.email = email;
        this.fechaNacimiento = fechaNacimiento;
        this.clinicaRut = clinicaRut;
    }

    // Getters y Setters
    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getClinicaRut() {
        return clinicaRut;
    }

    public void setClinicaRut(String clinicaRut) {
        this.clinicaRut = clinicaRut;
    }

    @Override
    public String toString() {
        return "RegistrarUsuarioRequest{" +
                "cedula='" + cedula + '\'' +
                ", tipoDocumento=" + tipoDocumento +
                ", primerNombre='" + primerNombre + '\'' +
                ", primerApellido='" + primerApellido + '\'' +
                ", email='" + email + '\'' +
                ", clinicaRut='" + clinicaRut + '\'' +
                '}';
    }
}
