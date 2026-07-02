package Edusecurity.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Leccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String descripcion;

    private String contenido;

    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private ModuloEducativo modulo;

    @OneToMany(mappedBy = "leccion")
    private List<ProgresoUsuario> progresos;

    // Constructor vacío
    public Leccion() {}

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public ModuloEducativo getModulo() {
        return modulo;
    }

    public void setModulo(ModuloEducativo modulo) {
        this.modulo = modulo;
    }

    public List<ProgresoUsuario> getProgresos() {
        return progresos;
    }

    public void setProgresos(List<ProgresoUsuario> progresos) {
        this.progresos = progresos;
    }
}