package edusecurity.simulacion.service;

import edusecurity.simulacion.entity.Accion;
import edusecurity.simulacion.entity.Clasificacion;
import edusecurity.simulacion.entity.Indicador;
import edusecurity.simulacion.entity.MensajeSimulado;
import edusecurity.simulacion.entity.RespuestaMensaje;
import edusecurity.simulacion.entity.TipoIndicador;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Califica una sesión de forma determinista. Por cada mensaje:
 * <ul>
 *   <li>{@value #PUNTOS_CLASIFICACION} puntos si lo clasifica bien (phishing o legítimo).</li>
 *   <li>{@value #PUNTOS_ACCION} puntos si elige la acción correcta; {@value #PUNTOS_ACCION_SEGURA}
 *       si elige otra acción segura.</li>
 *   <li>{@value #PUNTOS_POR_INDICADOR} puntos por cada señal de alerta real que marque, menos
 *       {@value #PENALIZACION_FALSO_INDICADOR} por cada señal que marque y no exista (mínimo 0).</li>
 * </ul>
 * Un mensaje sin responder vale 0. Se aprueba con {@value #PORCENTAJE_APROBACION} % o más.
 */
@Component
public class CalificadorSimulacion {

    static final int PUNTOS_CLASIFICACION = 10;
    static final int PUNTOS_ACCION = 3;
    static final int PUNTOS_ACCION_SEGURA = 1;
    static final int PUNTOS_POR_INDICADOR = 2;
    static final int PENALIZACION_FALSO_INDICADOR = 1;
    static final int PORCENTAJE_APROBACION = 70;
    private static final int MAX_SENALES_A_REFORZAR = 3;

    public ResultadoCalificacion calificar(List<MensajeSimulado> mensajes, Map<Long, RespuestaMensaje> respuestas) {
        List<EvaluacionMensaje> evaluaciones = mensajes.stream()
                .map(m -> evaluar(m, respuestas.get(m.getId())))
                .toList();

        int puntaje = evaluaciones.stream().mapToInt(EvaluacionMensaje::puntaje).sum();
        int maximo = evaluaciones.stream().mapToInt(EvaluacionMensaje::puntajeMaximo).sum();
        int porcentaje = maximo == 0 ? 0 : Math.round(puntaje * 100f / maximo);

        return new ResultadoCalificacion(evaluaciones, puntaje, maximo, porcentaje,
                porcentaje >= PORCENTAJE_APROBACION, senalesMasOmitidas(evaluaciones));
    }

    public int puntajeMaximo(MensajeSimulado mensaje) {
        return PUNTOS_CLASIFICACION + PUNTOS_ACCION + PUNTOS_POR_INDICADOR * mensaje.getIndicadores().size();
    }

    EvaluacionMensaje evaluar(MensajeSimulado mensaje, RespuestaMensaje respuesta) {
        Set<TipoIndicador> reales = EnumSet.noneOf(TipoIndicador.class);
        mensaje.getIndicadores().stream().map(Indicador::getTipo).forEach(reales::add);
        int maximo = puntajeMaximo(mensaje);

        if (respuesta == null) {
            return new EvaluacionMensaje(mensaje, null, false, false, false, false,
                    EnumSet.noneOf(TipoIndicador.class), reales, EnumSet.noneOf(TipoIndicador.class), 0, maximo);
        }

        boolean esPhishing = Boolean.TRUE.equals(mensaje.getEsPhishing());
        Clasificacion esperada = esPhishing ? Clasificacion.PHISHING : Clasificacion.LEGITIMO;
        boolean clasificacionCorrecta = respuesta.getClasificacion() == esperada;

        Accion accion = respuesta.getAccion();
        boolean accionCorrecta = accion == mensaje.getAccionCorrecta();
        boolean accionPeligrosa = esPhishing && accion.esPeligrosaAnteAtaque();

        Set<TipoIndicador> marcados = respuesta.getIndicadoresMarcados();
        Set<TipoIndicador> encontrados = interseccion(marcados, reales);
        Set<TipoIndicador> omitidos = diferencia(reales, marcados);
        Set<TipoIndicador> incorrectos = diferencia(marcados, reales);

        int puntaje = 0;
        if (clasificacionCorrecta) {
            puntaje += PUNTOS_CLASIFICACION;
        }
        if (accionCorrecta) {
            puntaje += PUNTOS_ACCION;
        } else if (esAccionSeguraAlternativa(accion, esPhishing)) {
            puntaje += PUNTOS_ACCION_SEGURA;
        }
        puntaje += Math.max(0, encontrados.size() * PUNTOS_POR_INDICADOR
                - incorrectos.size() * PENALIZACION_FALSO_INDICADOR);

        return new EvaluacionMensaje(mensaje, respuesta, true, clasificacionCorrecta, accionCorrecta,
                accionPeligrosa, encontrados, omitidos, incorrectos, Math.min(puntaje, maximo), maximo);
    }

    private boolean esAccionSeguraAlternativa(Accion accion, boolean esPhishing) {
        if (esPhishing) {
            return accion.esSeguraAnteAtaque();
        }
        // Ante un mensaje legítimo, verificarlo por otro canal es prudente aunque innecesario.
        return accion == Accion.VERIFICAR_POR_OTRO_CANAL;
    }

    private List<TipoIndicador> senalesMasOmitidas(List<EvaluacionMensaje> evaluaciones) {
        Map<TipoIndicador, Integer> conteo = new EnumMap<>(TipoIndicador.class);
        evaluaciones.forEach(e -> e.omitidos().forEach(t -> conteo.merge(t, 1, Integer::sum)));
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<TipoIndicador, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_SENALES_A_REFORZAR)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static Set<TipoIndicador> interseccion(Set<TipoIndicador> a, Set<TipoIndicador> b) {
        Set<TipoIndicador> r = copia(a);
        r.retainAll(b);
        return r;
    }

    private static Set<TipoIndicador> diferencia(Set<TipoIndicador> a, Set<TipoIndicador> b) {
        Set<TipoIndicador> r = copia(a);
        r.removeAll(b);
        return r;
    }

    private static Set<TipoIndicador> copia(Set<TipoIndicador> s) {
        return s == null || s.isEmpty() ? EnumSet.noneOf(TipoIndicador.class) : EnumSet.copyOf(s);
    }

    /** Resultado de un mensaje. {@code respuesta} es null si no se respondió. */
    public record EvaluacionMensaje(
            MensajeSimulado mensaje,
            RespuestaMensaje respuesta,
            boolean respondido,
            boolean clasificacionCorrecta,
            boolean accionCorrecta,
            boolean accionPeligrosa,
            Set<TipoIndicador> encontrados,
            Set<TipoIndicador> omitidos,
            Set<TipoIndicador> incorrectos,
            int puntaje,
            int puntajeMaximo) {
    }

    public record ResultadoCalificacion(
            List<EvaluacionMensaje> evaluaciones,
            int puntaje,
            int puntajeMaximo,
            int porcentaje,
            boolean aprobado,
            List<TipoIndicador> senalesAReforzar) {
    }
}
