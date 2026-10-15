package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.TipoIndicador;

/** Señal de alerta que el estudiante puede marcar. */
public record IndicadorCatalogo(TipoIndicador codigo, String etiqueta, String descripcion) {

    public static IndicadorCatalogo from(TipoIndicador tipo) {
        return new IndicadorCatalogo(tipo, tipo.getEtiqueta(), tipo.getDescripcion());
    }
}
