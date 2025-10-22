package com.hcen.frontend.usuario.bean;

import com.hcen.frontend.usuario.dto.profesional_salud_dto;
import com.hcen.frontend.usuario.service.api_service;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class profesional_bean {

    private String especialidad;
    private List<profesional_salud_dto> profesionales = new ArrayList<>();

    @Inject
    private api_service apiService;

    public void buscar() {
        if (especialidad != null && !especialidad.isBlank()) {
            profesionales = apiService.getProfesionalesByEspecialidad(especialidad.trim());
        } else {
            profesionales = new ArrayList<>();
        }
    }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public List<profesional_salud_dto> getProfesionales() { return profesionales; }
    public void setProfesionales(List<profesional_salud_dto> profesionales) { this.profesionales = profesionales; }
}
