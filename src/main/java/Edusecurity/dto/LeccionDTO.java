package Edusecurity.dto;

public class LeccionDTO {

    private Long id;
    private String titulo;
    private String contenido;
    private Long moduloId;

    public LeccionDTO() {
    }

    public LeccionDTO(Long id, String titulo, String contenido, Long moduloId) {
        this.id = id;
        this.titulo = titulo;
        this.contenido = contenido;
        this.moduloId = moduloId;
    }

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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public void setModuloId(Long moduloId) {
        this.moduloId = moduloId;
    }

}