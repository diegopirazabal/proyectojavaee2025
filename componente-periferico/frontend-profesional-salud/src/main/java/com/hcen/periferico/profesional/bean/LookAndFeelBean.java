package com.hcen.periferico.profesional.bean;

import com.hcen.periferico.profesional.dto.configuracion_clinica_dto;
import com.hcen.periferico.profesional.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@RequestScoped
public class LookAndFeelBean {

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    private String colorPrimario;
    private String colorSecundario;

    @PostConstruct
    public void init() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                colorPrimario = "#0d6efd";
                colorSecundario = "#6c757d";
                return;
            }

            configuracion_clinica_dto config = apiService.getConfiguracion(tenantId);

            if (config != null) {
                colorPrimario = (config.getColorPrimario() != null && !config.getColorPrimario().isEmpty())
                        ? config.getColorPrimario()
                        : "#0d6efd";

                colorSecundario = (config.getColorSecundario() != null && !config.getColorSecundario().isEmpty())
                        ? config.getColorSecundario()
                        : "#6c757d";
            } else {
                colorPrimario = "#0d6efd";
                colorSecundario = "#6c757d";
            }

        } catch (Exception e) {
            colorPrimario = "#0d6efd";
            colorSecundario = "#6c757d";
        }
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }
}