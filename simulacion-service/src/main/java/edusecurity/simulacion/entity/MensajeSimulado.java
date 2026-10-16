package edusecurity.simulacion.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Mensaje de la bandeja simulada. Los enlaces son texto: la plataforma nunca los abre
 * ni envía correos reales.
 */
@Entity
@Table(name = "mensajes_simulados")
public class MensajeSimulado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    @Column(nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Canal canal;

    @Column(nullable = false, length = 150)
    private String remitenteNombre;

    @Column(nullable = false, length = 200)
    private String remitenteDireccion;

    @Column(length = 200)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    /** Texto visible del enlace. */
    @Column(length = 300)
    private String enlaceMostrado;

    /** Destino real del enlace, el que vería el estudiante al pasar el cursor. */
    @Column(length = 300)
    private String enlaceReal;

    @Column(length = 150)
    private String adjuntoNombre;

    /** Fecha u hora tal como se muestra en la bandeja (p. ej. "Hoy, 10:42 a. m."). */
    @Column(length = 50)
    private String recibidoEn;

    @Column(nullable = false)
    private Boolean esPhishing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Accion accionCorrecta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explicacion;

    @OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Indicador> indicadores = new ArrayList<>();

    public void agregarIndicador(Indicador indicador) {
        indicador.setMensaje(this);
        indicadores.add(indicador);
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

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
    }

    public String getRemitenteDireccion() {
        return remitenteDireccion;
    }

    public void setRemitenteDireccion(String remitenteDireccion) {
        this.remitenteDireccion = remitenteDireccion;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public String getEnlaceMostrado() {
        return enlaceMostrado;
    }

    public void setEnlaceMostrado(String enlaceMostrado) {
        this.enlaceMostrado = enlaceMostrado;
    }

    public String getEnlaceReal() {
        return enlaceReal;
    }

    public void setEnlaceReal(String enlaceReal) {
        this.enlaceReal = enlaceReal;
    }

    public String getAdjuntoNombre() {
        return adjuntoNombre;
    }

    public void setAdjuntoNombre(String adjuntoNombre) {
        this.adjuntoNombre = adjuntoNombre;
    }

    public String getRecibidoEn() {
        return recibidoEn;
    }

    public void setRecibidoEn(String recibidoEn) {
        this.recibidoEn = recibidoEn;
    }

    public Boolean getEsPhishing() {
        return esPhishing;
    }

    public void setEsPhishing(Boolean esPhishing) {
        this.esPhishing = esPhishing;
    }

    public Accion getAccionCorrecta() {
        return accionCorrecta;
    }

    public void setAccionCorrecta(Accion accionCorrecta) {
        this.accionCorrecta = accionCorrecta;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public void setExplicacion(String explicacion) {
        this.explicacion = explicacion;
    }

    public List<Indicador> getIndicadores() {
        return indicadores;
    }
}
