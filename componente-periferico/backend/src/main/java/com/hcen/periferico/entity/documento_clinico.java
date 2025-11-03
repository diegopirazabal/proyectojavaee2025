package com.hcen.periferico.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "DOCUMENTO_CLINICO")
public class documento_clinico {

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

    // Historia Clínica - Esta entidad NO está en periférico
    // Si se necesita, se debe crear historia_clinica en periférico también
    // Por ahora, dejamos el campo comentado para evitar errores de compilación
    // @ManyToOne(optional = false, fetch = FetchType.LAZY)
    // @JoinColumn(name = "HIST_CLINICA_ID", nullable = false)
    // private historia_clinica historiaClinica;

    // Profesional (suscribe)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESIONAL_CI")
    private profesional_salud profesionalFirmante;

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFecCreacion() { return fecCreacion; }
    public void setFecCreacion(LocalDateTime fecCreacion) { this.fecCreacion = fecCreacion; }
    // public historia_clinica getHistoriaClinica() { return historiaClinica; }
    // public void setHistoriaClinica(historia_clinica historiaClinica) { this.historiaClinica = historiaClinica; }
    public profesional_salud getProfesionalFirmante() { return profesionalFirmante; }
    public void setProfesionalFirmante(profesional_salud p) { this.profesionalFirmante = p; }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof documento_clinico d && Objects.equals(id,d.id)); }
    @Override public int hashCode(){ return Objects.hash(id); }
}
