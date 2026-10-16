package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.EstadoSesion;
import java.time.LocalDateTime;
import java.util.List;

/** Estado de una sesión en curso, con la bandeja y el tiempo restante calculado por el servidor. */
public record SesionResponse(
        String uuid,
        EstadoSesion estado,
        LocalDateTime iniciadaEn,
        LocalDateTime venceEn,
        long segundosRestantes,
        int mensajesRespondidos,
        int totalMensajes,
        EscenarioDetalle escenario,
        List<MensajeBandeja> bandeja) {
}
