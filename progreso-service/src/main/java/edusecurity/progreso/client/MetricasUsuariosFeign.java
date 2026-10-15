package edusecurity.progreso.client;

import java.util.Map;

/** Métricas de usuarios devueltas por auth-service (GET /internal/usuarios/metricas). */
public record MetricasUsuariosFeign(
        long totalUsuarios,
        long usuariosActivos,
        Map<String, Long> porRol,
        long bloqueadosAhora,
        long usuariosConSesionEnPeriodo,
        long iniciosSesionEnPeriodo) {
}
