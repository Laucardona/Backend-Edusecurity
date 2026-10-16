package edusecurity.simulacion.dto;

/**
 * Consecuencia de una decisión. Si el laboratorio continúa, {@code siguientePaso} trae el nuevo
 * paso; si termina, {@code finalizada} es true y {@code siguientePaso} es null.
 */
public record AccionResponse(
        String pasoCodigo,
        String opcionCodigo,
        boolean correcta,
        int puntos,
        String retroalimentacion,
        int puntajeAcumulado,
        boolean finalizada,
        PasoResponse siguientePaso) {
}
