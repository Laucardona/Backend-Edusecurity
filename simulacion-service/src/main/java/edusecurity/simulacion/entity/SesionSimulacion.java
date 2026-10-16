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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Intento de un estudiante sobre un escenario, con tiempo límite controlado por el servidor. */
@Entity
@Table(name = "sesiones_simulacion")
public class SesionSimulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    /** Usuario de auth-service (sin FK entre servicios). */
    @Column(nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesion estado = EstadoSesion.EN_CURSO;

    @Column(nullable = false)
    private LocalDateTime iniciadaEn;

    @Column(nullable = false)
    private LocalDateTime venceEn;

    @Column
    private LocalDateTime finalizadaEn;

    @Column
    private Integer puntaje;

    @Column
    private Integer puntajeMaximo;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaMensaje> respuestas = new ArrayList<>();

    /** Paso en el que está el estudiante en un laboratorio guiado; null en phishing o al terminar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paso_actual_id")
    private PasoLaboratorio pasoActual;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AccionLaboratorio> acciones = new ArrayList<>();

    public SesionSimulacion() {
    }

    public SesionSimulacion(Escenario escenario, Long usuarioId, LocalDateTime ahora) {
        this.escenario = escenario;
        this.usuarioId = usuarioId;
        this.iniciadaEn = ahora;
        this.venceEn = ahora.plusSeconds(escenario.getDuracionSegundos());
    }

    public boolean estaVencida(LocalDateTime ahora) {
        return !ahora.isBefore(venceEn);
    }

    public boolean admiteRespuestas(LocalDateTime ahora) {
        return estado == EstadoSesion.EN_CURSO && !estaVencida(ahora);
    }

    public Optional<RespuestaMensaje> respuestaDe(Long mensajeId) {
        return respuestas.stream().filter(r -> r.getMensaje().getId().equals(mensajeId)).findFirst();
    }

    public void agregarRespuesta(RespuestaMensaje respuesta) {
        respuesta.setSesion(this);
        respuestas.add(respuesta);
    }

    public void agregarAccion(AccionLaboratorio accion) {
        accion.setSesion(this);
        acciones.add(accion);
    }

    public boolean pasoRespondido(Long pasoId) {
        return acciones.stream().anyMatch(a -> a.getPaso().getId().equals(pasoId));
    }

    public void cerrar(EstadoSesion estadoFinal, LocalDateTime ahora, int puntaje, int puntajeMaximo) {
        this.estado = estadoFinal;
        this.finalizadaEn = ahora;
        this.puntaje = puntaje;
        this.puntajeMaximo = puntajeMaximo;
    }

    public Long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Escenario getEscenario() {
        return escenario;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public EstadoSesion getEstado() {
        return estado;
    }

    public LocalDateTime getIniciadaEn() {
        return iniciadaEn;
    }

    public LocalDateTime getVenceEn() {
        return venceEn;
    }

    /** Solo para pruebas y tareas administrativas: ajusta el vencimiento. */
    public void setVenceEn(LocalDateTime venceEn) {
        this.venceEn = venceEn;
    }

    public LocalDateTime getFinalizadaEn() {
        return finalizadaEn;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public Integer getPuntajeMaximo() {
        return puntajeMaximo;
    }

    public List<RespuestaMensaje> getRespuestas() {
        return respuestas;
    }

    public PasoLaboratorio getPasoActual() {
        return pasoActual;
    }

    public void setPasoActual(PasoLaboratorio pasoActual) {
        this.pasoActual = pasoActual;
    }

    public List<AccionLaboratorio> getAcciones() {
        return acciones;
    }
}
