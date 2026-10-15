package edusecurity.contenido.dto;

/**
 * Referencia ligera al módulo al que pertenece una lección
 * (el contenido educativo no consulta la base de usuarios).
 */
public record ModuloResumen(Long id, String titulo) {
}
