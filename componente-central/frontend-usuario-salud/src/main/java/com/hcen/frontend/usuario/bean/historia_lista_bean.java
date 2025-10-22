package com.hcen.frontend.usuario.bean;

import com.hcen.frontend.usuario.dto.documento_historia_dto;
import com.hcen.frontend.usuario.service.api_service;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class historia_lista_bean implements Serializable {

    @Inject
    private login_bean loginBean;

    @Inject
    private api_service apiService;

    private List<documento_historia_dto> documentos;

    @PostConstruct
    public void init() {
        if (loginBean != null && loginBean.isLoggedIn() && loginBean.getCedula() != null) {
            documentos = apiService.listHistoria(loginBean.getCedula());
        }
    }

    public List<documento_historia_dto> getDocumentos() { return documentos; }
}

