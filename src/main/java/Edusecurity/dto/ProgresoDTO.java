package Edusecurity.dto;

public class ProgresoDTO {

    private Long id;
    private Double porcentajeCompletado;
    private Boolean completado;
    private Long usuarioId;
    private String usuarioNombre;
    private Long leccionId;
    private String leccionTitulo;

    public ProgresoDTO() {
    }

    public ProgresoDTO(Long id, Double porcentajeCompletado, Boolean completado,
                                Long usuarioId, String usuarioNombre,
                                Long leccionId, String leccionTitulo) {
        this.id = id;
        this.porcentajeCompletado = porcentajeCompletado;
        this.completado = completado;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.leccionId = leccionId;
        this.leccionTitulo = leccionTitulo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPorcentajeCompletado() { return porcentajeCompletado; }
    public void setPorcentajeCompletado(Double porcentajeCompletado) { this.porcentajeCompletado = porcentajeCompletado; }

    public Boolean getCompletado() { return completado; }
    public void setCompletado(Boolean completado) { this.completado = completado; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public Long getLeccionId() { return leccionId; }
    public void setLeccionId(Long leccionId) { this.leccionId = leccionId; }

    public String getLeccionTitulo() { return leccionTitulo; }
    public void setLeccionTitulo(String leccionTitulo) { this.leccionTitulo = leccionTitulo; }
}