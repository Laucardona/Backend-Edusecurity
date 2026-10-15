package edusecurity.contenido.dto;

import edusecurity.contenido.entity.Modulo;
import java.time.LocalDateTime;

public record ModuloResponse(
        Long id,
        String titulo,
        String descripcion,
        Boolean activo,
        Integer orden,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        Long totalLecciones) {

    public static ModuloResponse from(Modulo modulo, Long totalLecciones) {
        return new ModuloResponse(
                modulo.getId(),
                modulo.getTitulo(),
                modulo.getDescripcion(),
                modulo.getActivo(),
                modulo.getOrden(),
                modulo.getFechaCreacion(),
                modulo.getFechaActualizacion(),
                totalLecciones);
    }
}
