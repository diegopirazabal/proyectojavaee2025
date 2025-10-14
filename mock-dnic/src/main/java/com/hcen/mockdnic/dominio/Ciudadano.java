package com.hcen.mockdnic.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "ciudadanos")
public class Ciudadano {

    @EmbeddedId
    private CiudadanoId id;

    @Column(name = "nombre1", nullable = false, length = 80)
    private String primerNombre;

    @Column(name = "nombre2", length = 80)
    private String segundoNombre;

    @Column(name = "apellido1", nullable = false, length = 80)
    private String primerApellido;

    @Column(name = "apellido2", length = 80)
    private String segundoApellido;

    @Column(name = "sexo", nullable = false)
    private Integer sexo;

    @Column(name = "fecha_nac", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "cod_nacionalidad", nullable = false)
    private Integer codigoNacionalidad;

    @Column(name = "nombre_en_cedula", nullable = false, length = 200)
    private String nombreEnCedula;

    public CiudadanoId getId() {
        return id;
    }

    public void setId(CiudadanoId id) {
        this.id = id;
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

    public String getTipoDocumento() {
        return id != null ? id.getTipoDocumento() : null;
    }

    public String getNumeroDocumento() {
        return id != null ? id.getNumeroDocumento() : null;
    }
}
