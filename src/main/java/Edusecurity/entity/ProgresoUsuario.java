package Edusecurity.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "progreso_usuario")
public class ProgresoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double porcentajeCompletado;

    @Column(nullable = false)
    private Boolean completado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leccion_id", nullable = false)
    private Leccion leccion;

    public ProgresoUsuario() {
    }

    public ProgresoUsuario(Long id, Double porcentajeCompletado, Boolean completado,
                           Usuario usuario, Leccion leccion) {
        this.id = id;
        this.porcentajeCompletado = porcentajeCompletado;
        this.completado = completado;
        this.usuario = usuario;
        this.leccion = leccion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPorcentajeCompletado() {
        return porcentajeCompletado;
    }

    public void setPorcentajeCompletado(Double porcentajeCompletado) {
        this.porcentajeCompletado = porcentajeCompletado;
    }

    public Boolean getCompletado() {
        return completado;
    }

    public void setCompletado(Boolean completado) {
        this.completado = completado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Leccion getLeccion() {
        return leccion;
    }

    public void setLeccion(Leccion leccion) {
        this.leccion = leccion;
    }
}