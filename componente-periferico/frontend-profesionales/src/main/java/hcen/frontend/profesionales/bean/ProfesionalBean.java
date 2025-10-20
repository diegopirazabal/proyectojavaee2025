package hcen.frontend.profesionales.bean;

import hcen.frontend.profesionales.service.APIService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class ProfesionalBean implements Serializable {
    private String nombre;
    private String especialidad;
    private String username;

    @Inject
    private APIService api;

    public void cargarPerfil(String username) {
        this.username = username;
        // TODO: traer datos reales del profesional
        var perfil = api.obtenerPerfilProfesional(username);
        this.nombre = perfil.nombre();
        this.especialidad = perfil.especialidad();
    }

    // getters
    public String getNombre(){return nombre;}
    public String getEspecialidad(){return especialidad;}
    public String getUsername(){return username;}
}
