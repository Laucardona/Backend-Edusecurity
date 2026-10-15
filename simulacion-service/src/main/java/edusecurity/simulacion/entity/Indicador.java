package edusecurity.simulacion.entity;

import jakarta.persistence.Column;
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

/** Señal de alerta real de un mensaje, con el fragmento del texto que la delata. */
@Entity
@Table(name = "indicadores")
public class Indicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private MensajeSimulado mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoIndicador tipo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 300)
    private String fragmento;

    public Indicador() {
    }

    public Indicador(TipoIndicador tipo, String descripcion, String fragmento) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fragmento = fragmento;
    }

    public Long getId() {
        return id;
    }

    public MensajeSimulado getMensaje() {
        return mensaje;
    }

    void setMensaje(MensajeSimulado mensaje) {
        this.mensaje = mensaje;
    }

    public TipoIndicador getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getFragmento() {
        return fragmento;
    }
}
