package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Indicador;
import edusecurity.simulacion.entity.TipoIndicador;

/** Señal real de un mensaje con su explicación y el fragmento del texto que la delata. */
public record SenalDetalle(TipoIndicador codigo, String etiqueta, String descripcion, String fragmento) {

    public static SenalDetalle from(Indicador i) {
        return new SenalDetalle(i.getTipo(), i.getTipo().getEtiqueta(), i.getDescripcion(), i.getFragmento());
    }
}
