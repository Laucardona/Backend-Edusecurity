package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Canal;
import edusecurity.simulacion.entity.MensajeSimulado;
import edusecurity.simulacion.entity.RespuestaMensaje;

/**
 * Mensaje tal como lo ve el estudiante durante el laboratorio: sin indicar si es phishing.
 *
 * @param enlaceReal destino real del enlace (lo que se vería al pasar el cursor)
 * @param respuesta  lo que ya respondió, o null
 */
public record MensajeBandeja(
        Long id,
        int orden,
        Canal canal,
        String remitenteNombre,
        String remitenteDireccion,
        String asunto,
        String cuerpo,
        String enlaceMostrado,
        String enlaceReal,
        String adjuntoNombre,
        String recibidoEn,
        RespuestaResponse respuesta) {

    public static MensajeBandeja from(MensajeSimulado m, RespuestaMensaje respuesta) {
        return new MensajeBandeja(m.getId(), m.getOrden(), m.getCanal(), m.getRemitenteNombre(),
                m.getRemitenteDireccion(), m.getAsunto(), m.getCuerpo(), m.getEnlaceMostrado(),
                m.getEnlaceReal(), m.getAdjuntoNombre(), m.getRecibidoEn(),
                respuesta == null ? null : RespuestaResponse.from(respuesta));
    }
}
