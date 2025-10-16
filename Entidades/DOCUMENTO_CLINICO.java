package com.hcen.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "DOCUMENTO_CLINICO")
public class DocumentoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(length = 50)
    private String tipo;

    @Column(length = 200)
    private String titulo;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String contenido;

    @Column(name = "FEC_CREACION", nullable = false)
    private LocalDateTime fecCreacion = LocalDateTime.now();

    // Historia Clínica
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "HIST_CLINICA_ID", nullable = false)
    private HistoriaClinica historiaClinica;

    // Profesional (suscribe)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESIONAL_CI")
    private ProfesionalSalud profesionalFirmante;

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    public HistoriaClinica getHistoriaClinica() { return historiaClinica; }
    public void setHistoriaClinica(HistoriaClinica historiaClinica) { this.historiaClinica = historiaClinica; }
    public ProfesionalSalud getProfesionalFirmante() { return profesionalFirmante; }
    public void setProfesionalFirmante(ProfesionalSalud p) { this.profesionalFirmante = p; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof DocumentoClinico d && Objects.equals(id,d.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
