package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.Nivel;
import edusecurity.simulacion.entity.TipoEscenario;

public record EscenarioResumen(
        String uuid,
        TipoEscenario tipo,
        String titulo,
        String descripcion,
        Nivel nivel,
        int duracionSegundos,
        long totalMensajes,
        boolean publicado) {

    public static EscenarioResumen from(Escenario e, long totalMensajes) {
        return new EscenarioResumen(e.getUuid(), e.getTipo(), e.getTitulo(), e.getDescripcion(),
                e.getNivel(), e.getDuracionSegundos(), totalMensajes, Boolean.TRUE.equals(e.getPublicado()));
    }
}
