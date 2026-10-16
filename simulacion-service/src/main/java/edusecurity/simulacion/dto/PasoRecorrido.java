package edusecurity.simulacion.dto;

/**
 * Un paso del camino que siguió el estudiante, con lo que eligió y lo que era mejor.
 *
 * @param secuencia posición del paso en el recorrido (1, 2, 3...)
 */
public record PasoRecorrido(
        int secuencia,
        String pasoCodigo,
        String titulo,
        String opcionElegida,
        String textoElegido,
        boolean correcta,
        int puntos,
        String retroalimentacion,
        String mejorOpcion,
        String textoMejorOpcion,
        int puntosMejorOpcion,
        String retroalimentacionMejorOpcion) {
}
