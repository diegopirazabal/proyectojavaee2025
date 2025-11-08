package com.hcen.periferico.dto;

import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.rest.LocalDateAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serializable;
import java.time.LocalDate;
public class usuario_salud_dto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;  // Cambiado de Integer a String
    private TipoDocumento tipoDocumento;  // Nuevo campo
    private String primerNombre;  // Cambiado de 'nombre' a campos separados
    private String segundoNombre;  // Nuevo campo
    private String primerApellido;  // Cambiado de 'apellidos' a campos separados
    private String segundoApellido;  // Nuevo campo
    private String email;

    @JsonbTypeAdapter(LocalDateAdapter.class)
    private LocalDate fechaNacimiento;  // Cambiado de 'fechaNac'
    private String tenantId;

    // Constructores
    public usuario_salud_dto() {
    }

    public usuario_salud_dto(String cedula, TipoDocumento tipoDocumento, String primerNombre,
                            String primerApellido, String email, LocalDate fechaNacimiento) {
        this.cedula = cedula;
        this.tipoDocumento = tipoDocumento;
        this.primerNombre = primerNombre;
        this.primerApellido = primerApellido;
        this.email = email;
        this.fechaNacimiento = fechaNacimiento;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (primerNombre != null) sb.append(primerNombre);
        if (segundoNombre != null && !segundoNombre.isEmpty()) sb.append(" ").append(segundoNombre);
        if (primerApellido != null) sb.append(" ").append(primerApellido);
        if (segundoApellido != null && !segundoApellido.isEmpty()) sb.append(" ").append(segundoApellido);
        return sb.toString().trim();
    }
}
