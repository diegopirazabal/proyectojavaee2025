package com.hcen.mockdnic.ws.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "RespuestaCiudadano", namespace = "http://hcen.com/soap/ciudadano")
public class RespuestaCiudadano {

    @XmlElement(name = "tipoDoc", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private String tipoDocumento;

    @XmlElement(name = "nroDoc", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private String numeroDocumento;

    @XmlElement(name = "nombre1", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private String primerNombre;

    @XmlElement(name = "nombre2", namespace = "http://hcen.com/soap/ciudadano")
    private String segundoNombre;

    @XmlElement(name = "apellido1", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private String primerApellido;

    @XmlElement(name = "apellido2", namespace = "http://hcen.com/soap/ciudadano")
    private String segundoApellido;

    @XmlElement(name = "sexo", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private Integer sexo;

    @XmlElement(name = "fechaNac", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private String fechaNacimiento;

    @XmlElement(name = "codNacionalidad", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private Integer codigoNacionalidad;

    @XmlElement(name = "nombreEnCedula", required = true, namespace = "http://hcen.com/soap/ciudadano")
    private String nombreEnCedula;

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

    public Integer getSexo() {
        return sexo;
    }

    public void setSexo(Integer sexo) {
        this.sexo = sexo;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
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
}
