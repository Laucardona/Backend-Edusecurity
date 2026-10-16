package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Accion;
import edusecurity.simulacion.entity.Clasificacion;
import edusecurity.simulacion.entity.TipoIndicador;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Decisión del estudiante sobre un mensaje.
 *
 * @param indicadores señales de alerta que encontró (vacío si cree que es legítimo)
 */
public record RespuestaRequest(
        @NotNull Clasificacion clasificacion,
        @NotNull Accion accion,
        Set<TipoIndicador> indicadores) {
}
