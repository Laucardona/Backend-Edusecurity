package edusecurity.simulacion.client;

import java.time.LocalDateTime;

/** Resultado de una sesión cerrada, tal como lo recibe progreso-service. */
public record SimulacionCompletadaPayload(
        Long usuarioId,
        String sesionUuid,
        String escenarioUuid,
        String tipo,
        String titulo,
        int puntaje,
        int puntajeMaximo,
        double porcentaje,
        boolean aprobado,
        String estado,
        LocalDateTime finalizadaEn) {
}
