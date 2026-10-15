package edusecurity.progreso.dto;

import java.util.List;

/**
 * Estadísticas globales de la plataforma (panel administrativo).
 */
public record ResumenGlobalResponse(
        long totalRegistros,
        long totalUsuarios,
        long leccionesCompletadas,
        Double porcentajePromedio,
        List<ResumenModuloGlobalResponse> porModulo) {
}
