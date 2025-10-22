package com.hcen.frontend.usuario.bean;

import com.hcen.frontend.usuario.service.api_service;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Named
@RequestScoped
public class historia_doc_bean implements Serializable {

    @Inject
    private login_bean loginBean;

    @Inject
    private api_service apiService;

    public String getDocId() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("docId");
    }

    public String getPdfBase64() {
        String id = getDocId();
        if (id == null || loginBean == null || !loginBean.isLoggedIn()) return null;
        return apiService.fetchHistoriaPdfBase64(loginBean.getCedula(), id, true);
    }

    public String getHtmlDataUrl() {
        String id = getDocId();
        if (id == null || loginBean == null || !loginBean.isLoggedIn()) return null;
        String html = apiService.fetchHistoriaHtml(loginBean.getCedula(), id);
        if (html == null || html.isBlank()) return null;
        String b64 = Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        return "data:text/html;charset=UTF-8;base64," + b64;
    }
}

