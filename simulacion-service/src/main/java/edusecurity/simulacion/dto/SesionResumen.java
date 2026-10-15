package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.EstadoSesion;
import edusecurity.simulacion.entity.SesionSimulacion;
import java.time.LocalDateTime;

public record SesionResumen(
        String uuid,
        String escenarioUuid,
        String escenarioTitulo,
        EstadoSesion estado,
        LocalDateTime iniciadaEn,
        LocalDateTime finalizadaEn,
        Integer puntaje,
        Integer puntajeMaximo) {

    public static SesionResumen from(SesionSimulacion s) {
        return new SesionResumen(s.getUuid(), s.getEscenario().getUuid(), s.getEscenario().getTitulo(),
                s.getEstado(), s.getIniciadaEn(), s.getFinalizadaEn(), s.getPuntaje(), s.getPuntajeMaximo());
    }
}
