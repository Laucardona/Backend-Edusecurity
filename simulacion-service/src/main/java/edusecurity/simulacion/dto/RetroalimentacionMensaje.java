package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Accion;
import edusecurity.simulacion.entity.Clasificacion;
import java.util.List;

/** Retroalimentación de un mensaje al terminar el laboratorio. */
public record RetroalimentacionMensaje(
        Long mensajeId,
        int orden,
        String remitente,
        String asunto,
        boolean esPhishing,
        boolean respondido,
        Clasificacion tuClasificacion,
        boolean clasificacionCorrecta,
        Accion tuAccion,
        Accion accionRecomendada,
        boolean accionCorrecta,
        boolean accionPeligrosa,
        List<SenalDetalle> senalesEncontradas,
        List<SenalDetalle> senalesOmitidas,
        List<IndicadorCatalogo> senalesIncorrectas,
        String explicacion,
        int puntaje,
        int puntajeMaximo) {
}
