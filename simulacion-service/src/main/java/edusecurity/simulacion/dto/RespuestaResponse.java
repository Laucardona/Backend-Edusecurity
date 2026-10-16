package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Accion;
import edusecurity.simulacion.entity.Clasificacion;
import edusecurity.simulacion.entity.RespuestaMensaje;
import edusecurity.simulacion.entity.TipoIndicador;
import java.time.LocalDateTime;
import java.util.Set;

public record RespuestaResponse(
        Long mensajeId,
        Clasificacion clasificacion,
        Accion accion,
        Set<TipoIndicador> indicadores,
        LocalDateTime respondidaEn) {

    public static RespuestaResponse from(RespuestaMensaje r) {
        return new RespuestaResponse(r.getMensaje().getId(), r.getClasificacion(), r.getAccion(),
                r.getIndicadoresMarcados(), r.getRespondidaEn());
    }
}
