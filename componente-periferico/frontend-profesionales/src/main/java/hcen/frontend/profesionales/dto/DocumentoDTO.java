package hcen.frontend.profesionales.dto;

import java.time.LocalDate;

public class DocumentoDTO {
    private String id;
    private LocalDate fecha;
    private String tipo;
    private String origen;     // "LOCAL" o nombre de la clínica externa
    private boolean disponible;

    // getters/setters
    public String getId(){return id;}
    public void setId(String id){this.id=id;}
    public LocalDate getFecha(){return fecha;}
    public void setFecha(LocalDate f){this.fecha=f;}
    public String getTipo(){return tipo;}
    public void setTipo(String t){this.tipo=t;}
    public String getOrigen(){return origen;}
    public void setOrigen(String o){this.origen=o;}
    public boolean isDisponible(){return disponible;}
    public void setDisponible(boolean d){this.disponible=d;}
}
