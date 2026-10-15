package edusecurity.auth.dto;

import edusecurity.auth.entity.Sesion;
import java.time.Instant;

/**
 * Entrada del historial de sesiones (HU37): fecha, hora y dispositivo.
 */
public record SesionResponse(Long id, Instant inicio, Instant ultimoUso, String ip,
                             String dispositivo, boolean activa) {

    public static SesionResponse from(Sesion sesion, Instant ahora) {
        return new SesionResponse(sesion.getId(), sesion.getCreadaEn(), sesion.getUltimoUso(),
                sesion.getIp(), sesion.getDispositivo(), sesion.estaActiva(ahora));
    }
}
