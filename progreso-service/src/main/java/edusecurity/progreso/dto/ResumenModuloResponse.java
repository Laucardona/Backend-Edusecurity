package edusecurity.progreso.dto;

/**
 * Estadísticas de avance agrupadas por módulo educativo.
 */
public record ResumenModuloResponse(
        Long moduloId,
        String moduloTitulo,
        long totalLecciones,
        long leccionesCompletadas,
        Double porcentajePromedio) {
}
