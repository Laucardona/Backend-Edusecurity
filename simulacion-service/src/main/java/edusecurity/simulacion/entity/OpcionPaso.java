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

/** Decisión posible en un paso. Puntos, retroalimentación y siguiente paso no se muestran antes de elegir. */
@Entity
@Table(name = "opciones_paso")
public class OpcionPaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paso_id", nullable = false)
    private PasoLaboratorio paso;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private Integer puntos;

    @Column(nullable = false)
    private Boolean esCorrecta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String retroalimentacion;

    /** Código del paso al que lleva; null termina el laboratorio. */
    @Column(length = 40)
    private String siguientePasoCodigo;

    public OpcionPaso() {
    }

    public OpcionPaso(String codigo, String texto, int puntos, boolean esCorrecta, String retroalimentacion,
                      String siguientePasoCodigo) {
        this.codigo = codigo;
        this.texto = texto;
        this.puntos = puntos;
        this.esCorrecta = esCorrecta;
        this.retroalimentacion = retroalimentacion;
        this.siguientePasoCodigo = siguientePasoCodigo;
    }

    public Long getId() {
        return id;
    }

    public PasoLaboratorio getPaso() {
        return paso;
    }

    void setPaso(PasoLaboratorio paso) {
        this.paso = paso;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTexto() {
        return texto;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public Boolean getEsCorrecta() {
        return esCorrecta;
    }

    public String getRetroalimentacion() {
        return retroalimentacion;
    }

    public String getSiguientePasoCodigo() {
        return siguientePasoCodigo;
    }
}
