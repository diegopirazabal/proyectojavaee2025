package hcen.central.inus.entity;

import hcen.central.inus.enums.PoliticaAlcance;
import hcen.central.inus.enums.PoliticaEstado;
import hcen.central.inus.enums.TipoAcceso;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "politica_acceso")
public class politica_acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "acceso")
    private Boolean acceso;

    @Column(name = "fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "tipoAcceso", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'LECTURA'")
    private TipoAcceso tipoAcceso;

    @Column(name = "alcance", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'TODOS'")
    private PoliticaAlcance politicaAlcance;

    @Column(name = "estado", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVO'")
    private PoliticaEstado politicaEstado;

    //escribe getters y setters

}
