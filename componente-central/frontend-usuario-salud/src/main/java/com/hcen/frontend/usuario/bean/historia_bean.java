package com.hcen.frontend.usuario.bean;

import com.hcen.frontend.usuario.service.api_service;
import com.hcen.frontend.usuario.service.xslt_service;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.event.FileUploadEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Named
@ViewScoped
public class historia_bean implements Serializable {

    @Inject
    private api_service apiService;

    @Inject
    private login_bean loginBean;

    @Inject
    private xslt_service xsltService;

    private String xmlContent;
    private String xslContent;
    private String htmlContent;
    private String xmlFileName;
    private String xslFileName;

    public String getPdfBase64() {
        if (loginBean != null && loginBean.isLoggedIn() && loginBean.getCedula() != null) {
            return apiService.fetchHistoriaClinicaPdfBase64(loginBean.getCedula(), null);
        }
        return null;
    }

    public void handleXmlUpload(FileUploadEvent event) {
        try {
            this.xmlContent = readAll(event.getFile().getInputStream());
            this.xmlFileName = event.getFile().getFileName();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "XML cargado", xmlFileName));
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error leyendo XML", e.getMessage()));
        }
    }

    public void handleXslUpload(FileUploadEvent event) {
        try {
            this.xslContent = readAll(event.getFile().getInputStream());
            this.xslFileName = event.getFile().getFileName();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "XSL cargado", xslFileName));
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error leyendo XSL", e.getMessage()));
        }
    }

    public void transformar() {
        if (xmlContent == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Falta XML", "Cargue el XML de la historia."));
            return;
        }
        try {
            String xsl = this.xslContent;
            if (xsl == null || xsl.isBlank()) {
                String defaultXsl = loadResourceText("historia/cda.xsl");
                if (defaultXsl == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "No se encontró XSL", "Cargue XSL o incluya historia/cda.xsl en recursos."));
                    return;
                }
                xsl = defaultXsl;
            }
            this.htmlContent = xsltService.transform(xmlContent, xsl);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Transformación completa", null));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error transformando", e.getMessage()));
        }
    }

    public void cargarEjemplo() {
        try {
            String xml = loadResourceText("historia/ejemplo.xml");
            String xsl = loadResourceText("historia/cda.xsl");
            if (xml == null || xsl == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Archivos de ejemplo no encontrados",
                        "Mueva los ejemplos a src/main/resources/historia"));
                return;
            }
            this.xmlContent = xml;
            this.xslContent = xsl;
            this.xmlFileName = "ejemplo.xml";
            this.xslFileName = "cda.xsl";
            transformar();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No se pudo cargar el ejemplo", e.getMessage()));
        }
    }

    private String readAll(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String loadResourceText(String path) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = this.getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(path)) {
            if (is != null) return readAll(is);
        }
        return null;
    }

    public String getHtmlContent() { return htmlContent; }
    public String getXmlFileName() { return xmlFileName; }
    public String getXslFileName() { return xslFileName; }

    public String getHtmlDataUrl() {
        if (htmlContent == null || htmlContent.isBlank()) return null;
        byte[] bytes = htmlContent.getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return "data:text/html;charset=UTF-8;base64," + b64;
    }
}
