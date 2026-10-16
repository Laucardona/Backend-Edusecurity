package edusecurity.progreso.dto;

import edusecurity.progreso.entity.SimulacionCompletada;
import java.time.LocalDateTime;

public record SimulacionCompletadaResponse(
        Long id,
        Long usuarioId,
        String sesionUuid,
        String escenarioUuid,
        String tipo,
        String titulo,
        Integer puntaje,
        Integer puntajeMaximo,
        Double porcentaje,
        Boolean aprobado,
        String estado,
        LocalDateTime finalizadaEn,
        LocalDateTime fechaRegistro) {

    public static SimulacionCompletadaResponse from(SimulacionCompletada s) {
        return new SimulacionCompletadaResponse(s.getId(), s.getUsuarioId(), s.getSesionUuid(), s.getEscenarioUuid(),
                s.getTipo(), s.getTitulo(), s.getPuntaje(), s.getPuntajeMaximo(), s.getPorcentaje(), s.getAprobado(),
                s.getEstado(), s.getFinalizadaEn(), s.getFechaRegistro());
    }
}
