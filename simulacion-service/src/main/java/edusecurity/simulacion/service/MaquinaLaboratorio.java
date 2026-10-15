package edusecurity.simulacion.service;

import edusecurity.simulacion.entity.AccionLaboratorio;
import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.OpcionPaso;
import edusecurity.simulacion.entity.PasoLaboratorio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Máquina de estados del laboratorio guiado (HU44) y su calificación, de forma determinista:
 * <ul>
 *   <li>El paso inicial es el de menor orden.</li>
 *   <li>Elegir una opción lleva al paso indicado en {@code siguientePasoCodigo}; si es null o el
 *       paso es final, el laboratorio termina.</li>
 *   <li>El puntaje es la suma de los puntos de las opciones elegidas. El máximo es la suma del
 *       camino óptimo: desde el paso inicial, la mejor opción de cada paso.</li>
 *   <li>Se aprueba con {@value #PORCENTAJE_APROBACION} % o más.</li>
 * </ul>
 */
@Component
public class MaquinaLaboratorio {

    static final int PORCENTAJE_APROBACION = 70;

    /** Mejor opción: más puntos; a igual puntaje, la marcada como correcta y luego por código. */
    private static final Comparator<OpcionPaso> MEJOR_PRIMERO = Comparator
            .comparing(OpcionPaso::getPuntos, Comparator.reverseOrder())
            .thenComparing(o -> !Boolean.TRUE.equals(o.getEsCorrecta()))
            .thenComparing(OpcionPaso::getCodigo);

    public Optional<PasoLaboratorio> pasoInicial(Escenario escenario) {
        return escenario.getPasos().stream().min(Comparator.comparing(PasoLaboratorio::getOrden));
    }

    /** Paso al que lleva la opción, o vacío si el laboratorio termina. */
    public Optional<PasoLaboratorio> siguiente(PasoLaboratorio paso, OpcionPaso opcion) {
        if (Boolean.TRUE.equals(paso.getEsFinal()) || opcion.getSiguientePasoCodigo() == null) {
            return Optional.empty();
        }
        PasoLaboratorio siguiente = paso.getEscenario().paso(opcion.getSiguientePasoCodigo())
                .orElseThrow(() -> new IllegalStateException("La opción " + paso.getCodigo() + "-"
                        + opcion.getCodigo() + " apunta a un paso inexistente: " + opcion.getSiguientePasoCodigo()));
        return Optional.of(siguiente);
    }

    public OpcionPaso mejorOpcion(PasoLaboratorio paso) {
        return paso.getOpciones().stream().min(MEJOR_PRIMERO)
                .orElseThrow(() -> new IllegalStateException("El paso " + paso.getCodigo() + " no tiene opciones"));
    }

    /** Recorre el escenario eligiendo siempre la mejor opción. Un ciclo se corta al repetir un paso. */
    public List<Decision> caminoOptimo(Escenario escenario) {
        List<Decision> camino = new ArrayList<>();
        Set<String> visitados = new HashSet<>();
        Optional<PasoLaboratorio> actual = pasoInicial(escenario);
        while (actual.isPresent() && visitados.add(actual.get().getCodigo())) {
            PasoLaboratorio paso = actual.get();
            OpcionPaso mejor = mejorOpcion(paso);
            camino.add(new Decision(paso, mejor));
            actual = siguiente(paso, mejor);
        }
        return camino;
    }

    public int puntajeMaximo(Escenario escenario) {
        return caminoOptimo(escenario).stream().mapToInt(d -> d.opcion().getPuntos()).sum();
    }

    public Calificacion calificar(Escenario escenario, List<AccionLaboratorio> acciones) {
        int maximo = puntajeMaximo(escenario);
        // Un camino de recuperación puede sumar más pasos que el óptimo: el puntaje no supera el máximo.
        int puntaje = Math.min(maximo, acciones.stream().mapToInt(AccionLaboratorio::getPuntos).sum());
        int porcentaje = maximo == 0 ? 0 : Math.round(puntaje * 100f / maximo);
        return new Calificacion(puntaje, maximo, porcentaje, porcentaje >= PORCENTAJE_APROBACION);
    }

    public record Decision(PasoLaboratorio paso, OpcionPaso opcion) {
    }

    public record Calificacion(int puntaje, int puntajeMaximo, int porcentaje, boolean aprobado) {
    }
}
