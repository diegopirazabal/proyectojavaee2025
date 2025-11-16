package com.hcen.periferico.profesional.bean;

import com.hcen.periferico.profesional.dto.documento_clinico_dto;
import com.hcen.periferico.profesional.service.APIService;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permite a un profesional buscar la historia clínica de un paciente
 * en base a la cédula ingresada.
 */
@Named
@ViewScoped
public class HistoriaClinicaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    private String ci;
    private List<documento_clinico_dto> resultado = new ArrayList<>();

    public void buscar() {
        resultado = new ArrayList<>();

        if (ci == null || ci.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Debe ingresar la cédula del paciente");
            return;
        }

        String tenantId = sessionBean.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo determinar la clínica del profesional");
            return;
        }

        try {
            List<documento_clinico_dto> documentos = apiService.getDocumentosPorPaciente(
                ci.trim(), UUID.fromString(tenantId)
            );
            if (documentos != null) {
                resultado = documentos;
            }

            if (resultado.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_INFO, "No se encontraron registros para la cédula indicada");
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al consultar la historia clínica: " + e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, null));
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public List<documento_clinico_dto> getResultado() {
        return resultado;
    }
}
