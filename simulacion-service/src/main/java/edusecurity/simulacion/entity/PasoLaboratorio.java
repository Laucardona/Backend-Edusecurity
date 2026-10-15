package edusecurity.simulacion.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Estado de un laboratorio guiado: una situación, el recurso simulado que el estudiante
 * analiza (salida de terminal, registro, alerta o pantalla, como texto) y las opciones posibles.
 */
@Entity
@Table(name = "pasos_laboratorio")
public class PasoLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    /** Identificador del paso dentro del escenario; las opciones lo usan para indicar el siguiente. */
    @Column(nullable = false, length = 40)
    private String codigo;

    /** El paso de menor orden es el inicial. */
    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String situacion;

    @Column(columnDefinition = "TEXT")
    private String recursoSimulado;

    /** Responder un paso final cierra la sesión, sin importar la opción elegida. */
    @Column(nullable = false)
    private Boolean esFinal = false;

    @OneToMany(mappedBy = "paso", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("codigo ASC")
    private List<OpcionPaso> opciones = new ArrayList<>();

    public PasoLaboratorio() {
    }

    public PasoLaboratorio(String codigo, int orden, String titulo, String situacion, String recursoSimulado,
                           boolean esFinal) {
        this.codigo = codigo;
        this.orden = orden;
        this.titulo = titulo;
        this.situacion = situacion;
        this.recursoSimulado = recursoSimulado;
        this.esFinal = esFinal;
    }

    public void agregarOpcion(OpcionPaso opcion) {
        opcion.setPaso(this);
        opciones.add(opcion);
    }

    public Optional<OpcionPaso> opcion(String codigoOpcion) {
        return opciones.stream().filter(o -> o.getCodigo().equals(codigoOpcion)).findFirst();
    }

    public Long getId() {
        return id;
    }

    public Escenario getEscenario() {
        return escenario;
    }

    void setEscenario(Escenario escenario) {
        this.escenario = escenario;
    }

    public String getCodigo() {
        return codigo;
    }

    public Integer getOrden() {
        return orden;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSituacion() {
        return situacion;
    }

    public String getRecursoSimulado() {
        return recursoSimulado;
    }

    public Boolean getEsFinal() {
        return esFinal;
    }

    public List<OpcionPaso> getOpciones() {
        return opciones;
    }
}
