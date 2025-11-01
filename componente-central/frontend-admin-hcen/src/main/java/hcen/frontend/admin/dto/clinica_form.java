package hcen.frontend.admin.dto;

import java.io.Serializable;

public class clinica_form implements Serializable {

    private String nombre;
    private String direccion;
    private String email;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void reset() {
        nombre = null;
        direccion = null;
        email = null;
    }
}
