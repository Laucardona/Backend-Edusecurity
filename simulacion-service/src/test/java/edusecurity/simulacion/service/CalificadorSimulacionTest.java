package edusecurity.simulacion.service;

import static org.assertj.core.api.Assertions.assertThat;

import edusecurity.simulacion.entity.Accion;
import edusecurity.simulacion.entity.Canal;
import edusecurity.simulacion.entity.Clasificacion;
import edusecurity.simulacion.entity.Indicador;
import edusecurity.simulacion.entity.MensajeSimulado;
import edusecurity.simulacion.entity.RespuestaMensaje;
import edusecurity.simulacion.entity.TipoIndicador;
import edusecurity.simulacion.service.CalificadorSimulacion.EvaluacionMensaje;
import edusecurity.simulacion.service.CalificadorSimulacion.ResultadoCalificacion;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CalificadorSimulacionTest {

    private final CalificadorSimulacion calificador = new CalificadorSimulacion();

    @Test
    void phishingPerfectoObtieneElMaximo() {
        MensajeSimulado m = phishing(1L, TipoIndicador.URGENCIA, TipoIndicador.DOMINIO_SIMILAR);

        EvaluacionMensaje e = calificador.evaluar(m, respuesta(m, Clasificacion.PHISHING, Accion.REPORTAR,
                TipoIndicador.URGENCIA, TipoIndicador.DOMINIO_SIMILAR));

        assertThat(e.puntaje()).isEqualTo(17).isEqualTo(e.puntajeMaximo());
        assertThat(e.clasificacionCorrecta()).isTrue();
        assertThat(e.accionCorrecta()).isTrue();
        assertThat(e.omitidos()).isEmpty();
    }

    @Test
    void senalesFalsasRestanPeroNuncaBajanDeCero() {
        MensajeSimulado m = phishing(1L, TipoIndicador.URGENCIA);

        EvaluacionMensaje e = calificador.evaluar(m, respuesta(m, Clasificacion.PHISHING, Accion.REPORTAR,
                TipoIndicador.ADJUNTO_PELIGROSO, TipoIndicador.SECRETISMO, TipoIndicador.AMENAZA));

        // 10 por clasificar + 3 por la acción; señales: 0 aciertos - 3 falsas = 0 (no negativo)
        assertThat(e.puntaje()).isEqualTo(13);
        assertThat(e.omitidos()).containsExactly(TipoIndicador.URGENCIA);
        assertThat(e.incorrectos()).hasSize(3);
    }

    @Test
    void abrirElEnlaceDeUnPhishingEsPeligrosoYNoSumaPorAccion() {
        MensajeSimulado m = phishing(1L, TipoIndicador.ENLACE_ENGANOSO);

        EvaluacionMensaje e = calificador.evaluar(m, respuesta(m, Clasificacion.LEGITIMO, Accion.ABRIR_ENLACE));

        assertThat(e.puntaje()).isZero();
        assertThat(e.accionPeligrosa()).isTrue();
        assertThat(e.clasificacionCorrecta()).isFalse();
    }

    @Test
    void otraAccionSeguraDaPuntajeParcial() {
        MensajeSimulado m = phishing(1L, TipoIndicador.URGENCIA);

        EvaluacionMensaje e = calificador.evaluar(m, respuesta(m, Clasificacion.PHISHING, Accion.ELIMINAR,
                TipoIndicador.URGENCIA));

        assertThat(e.puntaje()).isEqualTo(10 + 1 + 2);
        assertThat(e.accionCorrecta()).isFalse();
        assertThat(e.accionPeligrosa()).isFalse();
    }

    @Test
    void marcarComoPhishingUnMensajeLegitimoEsFalsoPositivo() {
        MensajeSimulado m = legitimo(2L);

        EvaluacionMensaje e = calificador.evaluar(m, respuesta(m, Clasificacion.PHISHING, Accion.REPORTAR,
                TipoIndicador.URGENCIA));

        assertThat(e.puntaje()).isZero();
        assertThat(e.puntajeMaximo()).isEqualTo(13);
        assertThat(e.incorrectos()).containsExactly(TipoIndicador.URGENCIA);
    }

    @Test
    void mensajeSinResponderValeCeroYTodasSusSenalesQuedanOmitidas() {
        MensajeSimulado m = phishing(1L, TipoIndicador.URGENCIA, TipoIndicador.AMENAZA);

        EvaluacionMensaje e = calificador.evaluar(m, null);

        assertThat(e.respondido()).isFalse();
        assertThat(e.puntaje()).isZero();
        assertThat(e.omitidos()).containsExactlyInAnyOrder(TipoIndicador.URGENCIA, TipoIndicador.AMENAZA);
    }

    @Test
    void resultadoTotalCalculaPorcentajeAprobacionYSenalesAReforzar() {
        MensajeSimulado p1 = phishing(1L, TipoIndicador.URGENCIA, TipoIndicador.DOMINIO_SIMILAR);
        MensajeSimulado p2 = phishing(2L, TipoIndicador.URGENCIA);
        MensajeSimulado l1 = legitimo(3L);

        ResultadoCalificacion r = calificador.calificar(List.of(p1, p2, l1), Map.of(
                1L, respuesta(p1, Clasificacion.PHISHING, Accion.REPORTAR, TipoIndicador.DOMINIO_SIMILAR),
                3L, respuesta(l1, Clasificacion.LEGITIMO, Accion.CONSERVAR)));

        // p1: 10 + 3 + 2 = 15 de 17 · p2: sin responder, 0 de 15 · l1: 13 de 13
        assertThat(r.puntaje()).isEqualTo(28);
        assertThat(r.puntajeMaximo()).isEqualTo(45);
        assertThat(r.porcentaje()).isEqualTo(62);
        assertThat(r.aprobado()).isFalse();
        assertThat(r.senalesAReforzar()).first().isEqualTo(TipoIndicador.URGENCIA);
    }

    // ------------------------------------------------------------------ utilidades

    private static MensajeSimulado phishing(Long id, TipoIndicador... indicadores) {
        MensajeSimulado m = mensaje(id, true, Accion.REPORTAR);
        for (TipoIndicador t : indicadores) {
            m.agregarIndicador(new Indicador(t, "descripción", "fragmento"));
        }
        return m;
    }

    private static MensajeSimulado legitimo(Long id) {
        return mensaje(id, false, Accion.CONSERVAR);
    }

    private static MensajeSimulado mensaje(Long id, boolean esPhishing, Accion accion) {
        MensajeSimulado m = new MensajeSimulado();
        ReflectionTestUtils.setField(m, "id", id);
        m.setOrden(id.intValue());
        m.setCanal(Canal.CORREO);
        m.setRemitenteNombre("Remitente");
        m.setRemitenteDireccion("remitente@ejemplo.co");
        m.setCuerpo("Cuerpo");
        m.setEsPhishing(esPhishing);
        m.setAccionCorrecta(accion);
        m.setExplicacion("Explicación");
        return m;
    }

    private static RespuestaMensaje respuesta(MensajeSimulado m, Clasificacion c, Accion a, TipoIndicador... marcados) {
        RespuestaMensaje r = new RespuestaMensaje(m);
        r.actualizar(c, a, marcados.length == 0 ? EnumSet.noneOf(TipoIndicador.class) : EnumSet.of(marcados[0], marcados),
                LocalDateTime.now());
        return r;
    }
}
