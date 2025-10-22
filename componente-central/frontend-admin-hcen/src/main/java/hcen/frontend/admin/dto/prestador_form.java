package hcen.frontend.admin.dto;

import java.io.Serializable;

public class prestador_form implements Serializable {

    private String rut;
    private String nombre;
    private String email;

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void reset() {
        this.rut = null;
        this.nombre = null;
        this.email = null;
    }
}
