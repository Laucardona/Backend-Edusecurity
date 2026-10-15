package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.EstadoSesion;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado de una sesión en curso con el tiempo restante calculado por el servidor. En phishing trae
 * la bandeja; en un laboratorio guiado, el paso actual (null cuando la sesión terminó).
 */
public record SesionResponse(
        String uuid,
        EstadoSesion estado,
        LocalDateTime iniciadaEn,
        LocalDateTime venceEn,
        long segundosRestantes,
        int mensajesRespondidos,
        int totalMensajes,
        EscenarioDetalle escenario,
        List<MensajeBandeja> bandeja,
        int pasosRespondidos,
        PasoResponse pasoActual) {
}
