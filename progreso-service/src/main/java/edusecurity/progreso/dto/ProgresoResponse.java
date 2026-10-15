package edusecurity.progreso.dto;

import edusecurity.progreso.entity.Progreso;
import java.time.LocalDateTime;

/**
 * Progreso con los datos denormalizados consultados vía Feign
 * (nombre de usuario y título de lección/módulo).
 */
public record ProgresoResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        Long leccionId,
        String leccionTitulo,
        Long moduloId,
        String moduloTitulo,
        Double porcentaje,
        Boolean completado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public static ProgresoResponse of(Progreso progreso, String usuarioNombre,
                                      String leccionTitulo, String moduloTitulo) {
        return new ProgresoResponse(
                progreso.getId(),
                progreso.getUsuarioId(),
                usuarioNombre,
                progreso.getLeccionId(),
                leccionTitulo,
                progreso.getModuloId(),
                moduloTitulo,
                progreso.getPorcentaje(),
                progreso.getCompletado(),
                progreso.getFechaCreacion(),
                progreso.getFechaActualizacion());
    }
}
