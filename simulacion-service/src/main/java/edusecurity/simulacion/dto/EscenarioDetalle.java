package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.Nivel;
import edusecurity.simulacion.entity.TipoEscenario;

/** Información del escenario sin las respuestas correctas. */
public record EscenarioDetalle(
        String uuid,
        TipoEscenario tipo,
        String titulo,
        String descripcion,
        String contexto,
        Nivel nivel,
        int duracionSegundos,
        int totalMensajes,
        boolean publicado) {

    public static EscenarioDetalle from(Escenario e) {
        return new EscenarioDetalle(e.getUuid(), e.getTipo(), e.getTitulo(), e.getDescripcion(),
                e.getContexto(), e.getNivel(), e.getDuracionSegundos(), e.getMensajes().size(),
                Boolean.TRUE.equals(e.getPublicado()));
    }
}
