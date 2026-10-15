package edusecurity.simulacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edusecurity.simulacion.entity.AccionLaboratorio;
import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.OpcionPaso;
import edusecurity.simulacion.entity.PasoLaboratorio;
import edusecurity.simulacion.entity.TipoEscenario;
import edusecurity.simulacion.service.MaquinaLaboratorio.Calificacion;
import edusecurity.simulacion.service.MaquinaLaboratorio.Decision;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaquinaLaboratorioTest {

    private final MaquinaLaboratorio maquina = new MaquinaLaboratorio();

    /**
     * INICIO -A(10, correcta)-> MEDIO -A(10, correcta)-> FIN (final)
     *        -B(0)-----------> RECUPERACION -A(5)-> MEDIO
     *        -C(0)-----------> MEDIO
     * MEDIO  -B(2)-> FIN
     * FIN: A(10, correcta), B(0); el paso es final aunque la opción apunte a otro paso.
     */
    private Escenario escenario() {
        Escenario e = new Escenario();
        e.setTipo(TipoEscenario.LABORATORIO);

        // Se agregan desordenados para comprobar que el inicial es el de menor orden.
        PasoLaboratorio medio = paso("MEDIO", 3, false);
        medio.agregarOpcion(opcion("A", 10, true, "FIN"));
        medio.agregarOpcion(opcion("B", 2, false, "FIN"));

        PasoLaboratorio inicio = paso("INICIO", 1, false);
        inicio.agregarOpcion(opcion("A", 10, true, "MEDIO"));
        inicio.agregarOpcion(opcion("B", 0, false, "RECUPERACION"));
        inicio.agregarOpcion(opcion("C", 0, false, "MEDIO"));

        PasoLaboratorio recuperacion = paso("RECUPERACION", 2, false);
        recuperacion.agregarOpcion(opcion("A", 5, true, "MEDIO"));

        PasoLaboratorio fin = paso("FIN", 4, true);
        fin.agregarOpcion(opcion("A", 10, true, "INICIO"));
        fin.agregarOpcion(opcion("B", 0, false, null));

        e.agregarPaso(medio);
        e.agregarPaso(inicio);
        e.agregarPaso(recuperacion);
        e.agregarPaso(fin);
        return e;
    }

    @Test
    void elPasoInicialEsElDeMenorOrden() {
        assertThat(maquina.pasoInicial(escenario())).get().extracting(PasoLaboratorio::getCodigo).isEqualTo("INICIO");
    }

    @Test
    void unEscenarioSinPasosNoTienePasoInicial() {
        assertThat(maquina.pasoInicial(new Escenario())).isEmpty();
    }

    @Test
    void cadaOpcionLlevaAlPasoQueIndica() {
        Escenario e = escenario();
        PasoLaboratorio inicio = e.paso("INICIO").orElseThrow();

        assertThat(maquina.siguiente(inicio, inicio.opcion("A").orElseThrow())).get()
                .extracting(PasoLaboratorio::getCodigo).isEqualTo("MEDIO");
        assertThat(maquina.siguiente(inicio, inicio.opcion("B").orElseThrow())).get()
                .extracting(PasoLaboratorio::getCodigo).isEqualTo("RECUPERACION");
    }

    @Test
    void unPasoFinalOUnaOpcionSinSiguienteTerminanElLaboratorio() {
        Escenario e = escenario();
        PasoLaboratorio fin = e.paso("FIN").orElseThrow();

        // FIN es final: aunque la opción A apunte a INICIO, el laboratorio termina.
        assertThat(maquina.siguiente(fin, fin.opcion("A").orElseThrow())).isEmpty();
        assertThat(maquina.siguiente(fin, fin.opcion("B").orElseThrow())).isEmpty();
    }

    @Test
    void unaOpcionQueApuntaAUnPasoInexistenteEsUnErrorDeConfiguracion() {
        Escenario e = escenario();
        PasoLaboratorio medio = e.paso("MEDIO").orElseThrow();
        medio.agregarOpcion(opcion("Z", 0, false, "NO_EXISTE"));

        assertThatThrownBy(() -> maquina.siguiente(medio, medio.opcion("Z").orElseThrow()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NO_EXISTE");
    }

    @Test
    void laMejorOpcionEsLaDeMasPuntosYAEmpateLaCorrecta() {
        PasoLaboratorio p = paso("P", 1, true);
        p.agregarOpcion(opcion("A", 5, false, null));
        p.agregarOpcion(opcion("B", 5, true, null));
        p.agregarOpcion(opcion("C", 3, true, null));

        assertThat(maquina.mejorOpcion(p).getCodigo()).isEqualTo("B");
    }

    @Test
    void elMaximoEsLaSumaDelCaminoOptimoYNoDeTodasLasOpciones() {
        Escenario e = escenario();

        List<Decision> camino = maquina.caminoOptimo(e);

        assertThat(camino).extracting(d -> d.paso().getCodigo() + "-" + d.opcion().getCodigo())
                .containsExactly("INICIO-A", "MEDIO-A", "FIN-A");
        // RECUPERACION (5) no está en el camino óptimo.
        assertThat(maquina.puntajeMaximo(e)).isEqualTo(30);
    }

    @Test
    void elCaminoOptimoSeCortaSiHayUnCiclo() {
        Escenario e = new Escenario();
        PasoLaboratorio a = paso("A", 1, false);
        a.agregarOpcion(opcion("X", 4, true, "B"));
        PasoLaboratorio b = paso("B", 2, false);
        b.agregarOpcion(opcion("X", 6, true, "A"));
        e.agregarPaso(a);
        e.agregarPaso(b);

        assertThat(maquina.caminoOptimo(e)).hasSize(2);
        assertThat(maquina.puntajeMaximo(e)).isEqualTo(10);
    }

    @Test
    void elCaminoOptimoObtiene100PorCientoYAprueba() {
        Escenario e = escenario();

        Calificacion c = maquina.calificar(e, acciones(e, "INICIO-A", "MEDIO-A", "FIN-A"));

        assertThat(c.puntaje()).isEqualTo(30);
        assertThat(c.puntajeMaximo()).isEqualTo(30);
        assertThat(c.porcentaje()).isEqualTo(100);
        assertThat(c.aprobado()).isTrue();
    }

    @Test
    void seApruebaDesdeEl70PorCiento() {
        Escenario e = escenario();

        // 0 + 5 + 10 + 10 = 25 de 30 -> 83 %
        Calificacion recuperado = maquina.calificar(e, acciones(e, "INICIO-B", "RECUPERACION-A", "MEDIO-A", "FIN-A"));
        assertThat(recuperado.porcentaje()).isEqualTo(83);
        assertThat(recuperado.aprobado()).isTrue();

        // 0 + 10 + 10 = 20 de 30 -> 67 %
        Calificacion conError = maquina.calificar(e, acciones(e, "INICIO-C", "MEDIO-A", "FIN-A"));
        assertThat(conError.porcentaje()).isEqualTo(67);
        assertThat(conError.aprobado()).isFalse();
    }

    @Test
    void sinAccionesElPuntajeEsCero() {
        Calificacion c = maquina.calificar(escenario(), List.of());

        assertThat(c.puntaje()).isZero();
        assertThat(c.puntajeMaximo()).isEqualTo(30);
        assertThat(c.aprobado()).isFalse();
    }

    @Test
    void unCaminoMasLargoNoSuperaElMaximo() {
        Escenario e = new Escenario();
        PasoLaboratorio a = paso("A", 1, false);
        a.agregarOpcion(opcion("BUENA", 10, true, null));
        a.agregarOpcion(opcion("DESVIO", 8, false, "B"));
        PasoLaboratorio b = paso("B", 2, true);
        b.agregarOpcion(opcion("X", 8, true, null));
        e.agregarPaso(a);
        e.agregarPaso(b);

        Calificacion c = maquina.calificar(e, acciones(e, "A-DESVIO", "B-X"));

        assertThat(c.puntajeMaximo()).isEqualTo(10);
        assertThat(c.puntaje()).isEqualTo(10);
        assertThat(c.porcentaje()).isEqualTo(100);
    }

    // ------------------------------------------------------------------ utilidades

    private static PasoLaboratorio paso(String codigo, int orden, boolean esFinal) {
        return new PasoLaboratorio(codigo, orden, "Paso " + codigo, "Situación " + codigo, "recurso", esFinal);
    }

    private static OpcionPaso opcion(String codigo, int puntos, boolean correcta, String siguiente) {
        return new OpcionPaso(codigo, "Opción " + codigo, puntos, correcta, "Retro " + codigo, siguiente);
    }

    /** Cada elemento es "PASO-OPCION". */
    private static List<AccionLaboratorio> acciones(Escenario e, String... decisiones) {
        return java.util.Arrays.stream(decisiones).map(d -> {
            String[] partes = d.split("-");
            PasoLaboratorio paso = e.paso(partes[0]).orElseThrow();
            return new AccionLaboratorio(paso, paso.opcion(partes[1]).orElseThrow(), LocalDateTime.now());
        }).toList();
    }
}
