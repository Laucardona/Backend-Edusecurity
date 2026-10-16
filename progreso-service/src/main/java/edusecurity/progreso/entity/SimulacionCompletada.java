package edusecurity.progreso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Resultado de una simulación terminada (phishing o laboratorio guiado), recibido de
 * simulacion-service. Una fila por sesión.
 */
@Entity
@Table(name = "simulaciones_completadas")
public class SimulacionCompletada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 36)
    private String sesionUuid;

    @Column(nullable = false, length = 36)
    private String escenarioUuid;

    /** Tipo de escenario en simulacion-service (PHISHING_DIRIGIDO, LABORATORIO...). */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false)
    private Integer puntaje;

    @Column(nullable = false)
    private Integer puntajeMaximo;

    @Column(nullable = false)
    private Double porcentaje;

    @Column(nullable = false)
    private Boolean aprobado;

    /** FINALIZADA o VENCIDA. */
    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime finalizadaEn;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void onCreate() {
        fechaRegistro = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getSesionUuid() {
        return sesionUuid;
    }

    public void setSesionUuid(String sesionUuid) {
        this.sesionUuid = sesionUuid;
    }

    public String getEscenarioUuid() {
        return escenarioUuid;
    }

    public void setEscenarioUuid(String escenarioUuid) {
        this.escenarioUuid = escenarioUuid;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public Integer getPuntajeMaximo() {
        return puntajeMaximo;
    }

    public void setPuntajeMaximo(Integer puntajeMaximo) {
        this.puntajeMaximo = puntajeMaximo;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Boolean getAprobado() {
        return aprobado;
    }

    public void setAprobado(Boolean aprobado) {
        this.aprobado = aprobado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFinalizadaEn() {
        return finalizadaEn;
    }

    public void setFinalizadaEn(LocalDateTime finalizadaEn) {
        this.finalizadaEn = finalizadaEn;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
