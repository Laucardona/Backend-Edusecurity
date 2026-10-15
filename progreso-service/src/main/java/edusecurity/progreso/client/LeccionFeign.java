package edusecurity.progreso.client;

import java.time.LocalDateTime;

/**
 * Proyección mínima de la lección retornada por el servicio de contenido.
 */
public record LeccionFeign(
        Long id,
        String titulo,
        Boolean activo,
        Integer orden,
        LocalDateTime fechaCreacion,
        ModuloFeign modulo) {

    public record ModuloFeign(Long id, String titulo, String descripcion, Boolean activo,
                              Integer orden, LocalDateTime fechaCreacion,
                              LocalDateTime fechaActualizacion, Long totalLecciones) {
    }
}
