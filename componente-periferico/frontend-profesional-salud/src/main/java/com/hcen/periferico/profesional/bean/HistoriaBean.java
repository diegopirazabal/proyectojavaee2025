package com.hcen.periferico.profesional.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class HistoriaBean {
    private String cedula;
    private List<ItemDoc> documentos;

    @PostConstruct
    public void init(){ documentos = new ArrayList<>(); }

    public void buscar() {
        documentos.clear();
        if (cedula != null && !cedula.isBlank()) {
            documentos.add(new ItemDoc("Evolución médica", "EVOLUCION", "Local"));
            documentos.add(new ItemDoc("Laboratorio - Hemograma", "LAB", "Central"));
        }
    }

    public String solicitarAcceso(ItemDoc d) {
        return null;
    }

    public String volver() { return "/pages/dashboard.xhtml?faces-redirect=true"; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public List<ItemDoc> getDocumentos() { return documentos; }

    public static class ItemDoc {
        private String titulo; private String tipo; private String origen;
        public ItemDoc() {}
        public ItemDoc(String titulo, String tipo, String origen){ this.titulo=titulo; this.tipo=tipo; this.origen=origen; }
        public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
        public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
        public String getOrigen(){return origen;} public void setOrigen(String v){origen=v;}
    }
}

