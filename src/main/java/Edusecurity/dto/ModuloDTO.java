package Edusecurity.dto;

public class ModuloDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private Boolean activo;

    public ModuloDTO() {
    }

    public ModuloDTO(Long id, String titulo, String descripcion, Boolean activo) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}