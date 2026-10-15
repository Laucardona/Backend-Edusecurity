package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.OpcionPaso;
import edusecurity.simulacion.entity.PasoLaboratorio;
import java.util.List;

/**
 * Paso del laboratorio guiado tal como lo ve el estudiante: sin puntos, sin cuál opción es la
 * correcta, sin retroalimentación y sin a dónde lleva cada opción.
 *
 * @param recursoSimulado salida de terminal, registro, alerta o pantalla, como texto
 */
public record PasoResponse(
        String codigo,
        int orden,
        String titulo,
        String situacion,
        String recursoSimulado,
        boolean esFinal,
        List<Opcion> opciones) {

    public static PasoResponse from(PasoLaboratorio p) {
        return new PasoResponse(p.getCodigo(), p.getOrden(), p.getTitulo(), p.getSituacion(), p.getRecursoSimulado(),
                Boolean.TRUE.equals(p.getEsFinal()), p.getOpciones().stream().map(Opcion::from).toList());
    }

    public record Opcion(String codigo, String texto) {

        static Opcion from(OpcionPaso o) {
            return new Opcion(o.getCodigo(), o.getTexto());
        }
    }
}
