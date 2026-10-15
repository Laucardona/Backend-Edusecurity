package edusecurity.contenido.dto;

import edusecurity.contenido.entity.Leccion;
import java.time.LocalDateTime;

public record LeccionResponse(
        Long id,
        String titulo,
        String descripcion,
        String contenido,
        Integer orden,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        ModuloResumen modulo) {

    public static LeccionResponse from(Leccion leccion) {
        ModuloResumen modulo = new ModuloResumen(
                leccion.getModulo().getId(),
                leccion.getModulo().getTitulo());
        return new LeccionResponse(
                leccion.getId(),
                leccion.getTitulo(),
                leccion.getDescripcion(),
                leccion.getContenido(),
                leccion.getOrden(),
                leccion.getActivo(),
                leccion.getFechaCreacion(),
                leccion.getFechaActualizacion(),
                modulo);
    }
}
