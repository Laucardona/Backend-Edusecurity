package edusecurity.simulacion.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Escenario de simulación con un contexto de trabajo. Los de phishing tienen una bandeja de
 * mensajes simulados; los de tipo LABORATORIO, una secuencia de pasos con decisiones.
 */
@Entity
@Table(name = "escenarios")
public class Escenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador público usado en las URL. */
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEscenario tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    /** Rol y situación que asume el estudiante durante el laboratorio. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contexto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nivel nivel;

    @Column(nullable = false)
    private Integer duracionSegundos;

    @Column(nullable = false)
    private Boolean publicado = false;

    @OneToMany(mappedBy = "escenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<MensajeSimulado> mensajes = new ArrayList<>();

    /** Pasos del laboratorio guiado (solo tipo LABORATORIO). */
    @OneToMany(mappedBy = "escenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PasoLaboratorio> pasos = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public void agregarMensaje(MensajeSimulado mensaje) {
        mensaje.setEscenario(this);
        mensajes.add(mensaje);
    }

    public void agregarPaso(PasoLaboratorio paso) {
        paso.setEscenario(this);
        pasos.add(paso);
    }

    public Optional<PasoLaboratorio> paso(String codigo) {
        return pasos.stream().filter(p -> p.getCodigo().equals(codigo)).findFirst();
    }

    public boolean esLaboratorioGuiado() {
        return tipo != null && tipo.esLaboratorioGuiado();
    }

    public Long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public TipoEscenario getTipo() {
        return tipo;
    }

    public void setTipo(TipoEscenario tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public Nivel getNivel() {
        return nivel;
    }

    public void setNivel(Nivel nivel) {
        this.nivel = nivel;
    }

    public Integer getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Integer duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public Boolean getPublicado() {
        return publicado;
    }

    public void setPublicado(Boolean publicado) {
        this.publicado = publicado;
    }

    public List<MensajeSimulado> getMensajes() {
        return mensajes;
    }

    public List<PasoLaboratorio> getPasos() {
        return pasos;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
