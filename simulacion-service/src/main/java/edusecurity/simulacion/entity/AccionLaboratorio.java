package edusecurity.simulacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Decisión tomada en un paso del laboratorio guiado. Cada paso se responde una sola vez. */
@Entity
@Table(name = "acciones_laboratorio")
public class AccionLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionSimulacion sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paso_id", nullable = false)
    private PasoLaboratorio paso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcion_id", nullable = false)
    private OpcionPaso opcion;

    /** Puntos obtenidos, copiados de la opción al momento de responder. */
    @Column(nullable = false)
    private Integer puntos;

    @Column(nullable = false)
    private LocalDateTime ocurridaEn;

    public AccionLaboratorio() {
    }

    public AccionLaboratorio(PasoLaboratorio paso, OpcionPaso opcion, LocalDateTime ocurridaEn) {
        this.paso = paso;
        this.opcion = opcion;
        this.puntos = opcion.getPuntos();
        this.ocurridaEn = ocurridaEn;
    }

    public Long getId() {
        return id;
    }

    public SesionSimulacion getSesion() {
        return sesion;
    }

    void setSesion(SesionSimulacion sesion) {
        this.sesion = sesion;
    }

    public PasoLaboratorio getPaso() {
        return paso;
    }

    public OpcionPaso getOpcion() {
        return opcion;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public LocalDateTime getOcurridaEn() {
        return ocurridaEn;
    }
}
