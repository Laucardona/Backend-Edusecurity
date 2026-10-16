package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.EstadoSesion;
import edusecurity.simulacion.entity.TipoEscenario;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de una sesión terminada. Los campos de mensajes y señales aplican a phishing; en un
 * laboratorio guiado van en cero o vacíos y el detalle está en {@code recorrido}.
 *
 * @param falsosPositivos  mensajes legítimos que marcó como phishing
 * @param accionesPeligrosas mensajes de phishing en los que abrió el enlace o respondió
 * @param senalesAReforzar señales que más pasó por alto
 * @param recorrido        pasos del laboratorio guiado en el orden en que los respondió
 */
public record ResultadoResponse(
        String sesionUuid,
        String escenarioUuid,
        String escenarioTitulo,
        TipoEscenario tipo,
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
        List<RetroalimentacionMensaje> mensajes,
        List<PasoRecorrido> recorrido) {
}
