package edusecurity.progreso.analitica.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Panel administrativo (HU33). Cada bloque viene de una fuente; si una fuente externa no responde,
 * su bloque es null y su nombre aparece en {@code fuentesNoDisponibles}.
 *
 * @param usuarios     de auth-service; null si no respondió
 * @param progreso     estado actual del avance en lecciones (no se filtra por periodo)
 * @param simulaciones simulaciones terminadas dentro del periodo
 * @param certificados siempre null por ahora: la emisión de certificados (RF22) no está implementada
 */
public record PanelAdministrativoResponse(
        Periodo periodo,
        Usuarios usuarios,
        Progreso progreso,
        Simulaciones simulaciones,
        Object certificados,
        List<String> fuentesNoDisponibles) {

    /** Ambos días incluidos, en UTC. */
    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    public record Usuarios(
            long totalUsuarios,
            long usuariosActivos,
            Map<String, Long> porRol,
            long bloqueadosAhora,
            long usuariosConSesionEnPeriodo,
            long iniciosSesionEnPeriodo) {
    }

    public record Progreso(
            double promedioPorcentaje,
            long leccionesCompletadas,
            long estudiantesConProgreso,
            List<ProgresoModulo> porModulo) {
    }

    public record ProgresoModulo(Long moduloId, double promedio, long completadas) {
    }

    /**
     * @param tasaAprobacion porcentaje de aprobadas sobre completadas (0 si no hay)
     * @param porTipo        claves PHISHING y LABORATORIO
     * @param topEscenarios  los 5 escenarios con más simulaciones terminadas
     */
    public record Simulaciones(
            long completadas,
            long aprobadas,
            double tasaAprobacion,
            Map<String, SimulacionesTipo> porTipo,
            List<EscenarioTop> topEscenarios) {
    }

    public record SimulacionesTipo(long completadas, long aprobadas, double promedioPorcentaje) {
    }

    public record EscenarioTop(String escenarioUuid, String titulo, long completadas, double tasaAprobacion) {
    }
}
