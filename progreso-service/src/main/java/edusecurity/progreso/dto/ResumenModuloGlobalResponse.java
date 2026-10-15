package edusecurity.progreso.dto;

/**
 * Estadísticas globales por módulo (todos los usuarios).
 */
public record ResumenModuloGlobalResponse(
        Long moduloId,
        String moduloTitulo,
        long totalLecciones,
        long progresosRegistrados,
        long progresosCompletados,
        Double porcentajePromedio) {
}
