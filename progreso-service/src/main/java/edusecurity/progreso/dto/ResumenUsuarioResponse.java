package edusecurity.progreso.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen completo del progreso de un usuario.
 */
public record ResumenUsuarioResponse(
        Long usuarioId,
        String usuarioNombre,
        long totalLecciones,
        long leccionesRegistradas,
        long leccionesCompletadas,
        Double porcentajePromedio,
        LocalDateTime fechaUltimaActividad,
        List<ResumenModuloResponse> porModulo) {
}
