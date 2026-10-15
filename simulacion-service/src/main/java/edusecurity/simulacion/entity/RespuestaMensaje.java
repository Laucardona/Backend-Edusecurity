package edusecurity.simulacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Decisión del estudiante sobre un mensaje: se puede cambiar hasta finalizar la sesión. */
@Entity
@Table(name = "respuestas_mensaje")
public class RespuestaMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionSimulacion sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private MensajeSimulado mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Clasificacion clasificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Accion accion;

    @Convert(converter = IndicadoresConverter.class)
    @Column(length = 600)
    private Set<TipoIndicador> indicadoresMarcados = EnumSet.noneOf(TipoIndicador.class);

    @Column(nullable = false)
    private LocalDateTime respondidaEn;

    public RespuestaMensaje() {
    }

    public RespuestaMensaje(MensajeSimulado mensaje) {
        this.mensaje = mensaje;
    }

    public void actualizar(Clasificacion clasificacion, Accion accion,
                           Set<TipoIndicador> indicadores, LocalDateTime ahora) {
        this.clasificacion = clasificacion;
        this.accion = accion;
        this.indicadoresMarcados = indicadores == null || indicadores.isEmpty()
                ? EnumSet.noneOf(TipoIndicador.class)
                : EnumSet.copyOf(indicadores);
        this.respondidaEn = ahora;
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

    public MensajeSimulado getMensaje() {
        return mensaje;
    }

    public Clasificacion getClasificacion() {
        return clasificacion;
    }

    public Accion getAccion() {
        return accion;
    }

    public Set<TipoIndicador> getIndicadoresMarcados() {
        return indicadoresMarcados;
    }

    public LocalDateTime getRespondidaEn() {
        return respondidaEn;
    }
}
