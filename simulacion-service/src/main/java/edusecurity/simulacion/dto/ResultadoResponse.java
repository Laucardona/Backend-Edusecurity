package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.EstadoSesion;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de una sesión terminada.
 *
 * @param falsosPositivos  mensajes legítimos que marcó como phishing
 * @param accionesPeligrosas mensajes de phishing en los que abrió el enlace o respondió
 * @param senalesAReforzar señales que más pasó por alto
 */
public record ResultadoResponse(
        String sesionUuid,
        String escenarioUuid,
        String escenarioTitulo,
        EstadoSesion estado,
        LocalDateTime finalizadaEn,
        int puntaje,
        int puntajeMaximo,
        int porcentaje,
        boolean aprobado,
        int mensajesRespondidos,
        int totalMensajes,
        int phishingDetectados,
        int totalPhishing,
        int falsosPositivos,
        int accionesPeligrosas,
        List<IndicadorCatalogo> senalesAReforzar,
        List<RetroalimentacionMensaje> mensajes) {
}
