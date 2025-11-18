package hcen.central.inus.dto;

import hcen.central.inus.enums.TipoDocumento;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para transferir datos de ciudadano obtenidos desde DNIC.
 *
 * Este DTO mapea la respuesta del servicio SOAP de DNIC a nuestro modelo de dominio,
 * facilitando la integración con el sistema de usuarios de salud.
 */
public class DnicCiudadanoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Integer sexo;
    private LocalDate fechaNacimiento;
    private Integer codigoNacionalidad;
    private String nombreEnCedula;

    // Constructores
    public DnicCiudadanoDTO() {}

    public DnicCiudadanoDTO(TipoDocumento tipoDocumento, String numeroDocumento,
                            String primerNombre, String primerApellido,
                            LocalDate fechaNacimiento) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.primerNombre = primerNombre;
        this.primerApellido = primerApellido;
        this.fechaNacimiento = fechaNacimiento;
    }

    // Getters y Setters
    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
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

    public Integer getSexo() {
        return sexo;
    }

    public void setSexo(Integer sexo) {
        this.sexo = sexo;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Integer getCodigoNacionalidad() {
        return codigoNacionalidad;
    }

    public void setCodigoNacionalidad(Integer codigoNacionalidad) {
        this.codigoNacionalidad = codigoNacionalidad;
    }

    public String getNombreEnCedula() {
        return nombreEnCedula;
    }

    public void setNombreEnCedula(String nombreEnCedula) {
        this.nombreEnCedula = nombreEnCedula;
    }

    /**
     * Construye el nombre completo combinando nombres y apellidos.
     *
     * @return Nombre completo del ciudadano
     */
    public String getNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder();

        if (primerNombre != null && !primerNombre.isBlank()) {
            nombreCompleto.append(primerNombre);
        }
        if (segundoNombre != null && !segundoNombre.isBlank()) {
            if (nombreCompleto.length() > 0) {
                nombreCompleto.append(" ");
            }
            nombreCompleto.append(segundoNombre);
        }
        if (primerApellido != null && !primerApellido.isBlank()) {
            if (nombreCompleto.length() > 0) {
                nombreCompleto.append(" ");
            }
            nombreCompleto.append(primerApellido);
        }
        if (segundoApellido != null && !segundoApellido.isBlank()) {
            if (nombreCompleto.length() > 0) {
                nombreCompleto.append(" ");
            }
            nombreCompleto.append(segundoApellido);
        }

        return nombreCompleto.toString();
    }

    @Override
    public String toString() {
        return "DnicCiudadanoDTO{" +
                "tipoDocumento=" + tipoDocumento +
                ", numeroDocumento='" + numeroDocumento + '\'' +
                ", primerNombre='" + primerNombre + '\'' +
                ", segundoNombre='" + segundoNombre + '\'' +
                ", primerApellido='" + primerApellido + '\'' +
                ", segundoApellido='" + segundoApellido + '\'' +
                ", sexo=" + sexo +
                ", fechaNacimiento=" + fechaNacimiento +
                ", codigoNacionalidad=" + codigoNacionalidad +
                ", nombreEnCedula='" + nombreEnCedula + '\'' +
                '}';
    }
}
