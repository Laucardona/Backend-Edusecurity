package edusecurity.simulacion.service;

import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import edusecurity.simulacion.dto.EscenarioDetalle;
import edusecurity.simulacion.dto.IndicadorCatalogo;
import edusecurity.simulacion.dto.MensajeBandeja;
import edusecurity.simulacion.dto.RespuestaRequest;
import edusecurity.simulacion.dto.RespuestaResponse;
import edusecurity.simulacion.dto.ResultadoResponse;
import edusecurity.simulacion.dto.RetroalimentacionMensaje;
import edusecurity.simulacion.dto.SenalDetalle;
import edusecurity.simulacion.dto.SesionResponse;
import edusecurity.simulacion.dto.SesionResumen;
import edusecurity.simulacion.entity.Clasificacion;
import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.EstadoSesion;
import edusecurity.simulacion.entity.Indicador;
import edusecurity.simulacion.entity.MensajeSimulado;
import edusecurity.simulacion.entity.RespuestaMensaje;
import edusecurity.simulacion.entity.SesionSimulacion;
import edusecurity.simulacion.entity.TipoIndicador;
import edusecurity.simulacion.repository.SesionSimulacionRepository;
import edusecurity.simulacion.service.CalificadorSimulacion.EvaluacionMensaje;
import edusecurity.simulacion.service.CalificadorSimulacion.ResultadoCalificacion;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo del laboratorio: iniciar, responder cada mensaje, finalizar y ver la retroalimentación.
 * El tiempo límite se controla en el servidor; el reloj del cliente no cuenta.
 */
@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    private final SesionSimulacionRepository sesionRepository;
    private final EscenarioService escenarioService;
    private final CalificadorSimulacion calificador;
    private final Clock clock;

    public SesionService(SesionSimulacionRepository sesionRepository, EscenarioService escenarioService,
                         CalificadorSimulacion calificador, Clock clock) {
        this.sesionRepository = sesionRepository;
        this.escenarioService = escenarioService;
        this.calificador = calificador;
        this.clock = clock;
    }

    /**
     * Inicia el laboratorio. Si el estudiante ya tiene una sesión en curso de este escenario,
     * la devuelve en lugar de crear otra (recargar la página no reinicia el reloj).
     */
    @Transactional
    public SesionResponse iniciar(String escenarioUuid, TokenInfo principal) {
        Escenario escenario = escenarioService.buscarVisible(escenarioUuid, principal);
        LocalDateTime ahora = ahora();

        var enCurso = sesionRepository.findFirstByEscenarioIdAndUsuarioIdAndEstadoOrderByIniciadaEnDesc(
                escenario.getId(), principal.usuarioId(), EstadoSesion.EN_CURSO);
        if (enCurso.isPresent()) {
            SesionSimulacion sesion = enCurso.get();
            if (!sesion.estaVencida(ahora)) {
                return aRespuesta(sesion, ahora);
            }
            cerrar(sesion, ahora);
        }

        SesionSimulacion nueva = sesionRepository.save(new SesionSimulacion(escenario, principal.usuarioId(), ahora));
        log.info("Sesión de simulación iniciada: sesion={} escenario={} usuario={}",
                nueva.getUuid(), escenario.getUuid(), principal.usuarioId());
        return aRespuesta(nueva, ahora);
    }

    @Transactional
    public SesionResponse obtener(String sesionUuid, TokenInfo principal) {
        SesionSimulacion sesion = buscarLectura(sesionUuid, principal);
        LocalDateTime ahora = ahora();
        cerrarSiVencio(sesion, ahora);
        return aRespuesta(sesion, ahora);
    }

    /** Guarda o cambia la decisión sobre un mensaje mientras la sesión siga abierta. */
    @Transactional(noRollbackFor = ConflictException.class)
    public RespuestaResponse responder(String sesionUuid, Long mensajeId, RespuestaRequest request, TokenInfo principal) {
        SesionSimulacion sesion = buscarPropia(sesionUuid, principal);
        LocalDateTime ahora = ahora();

        if (cerrarSiVencio(sesion, ahora)) {
            throw new ConflictException("Se agotó el tiempo del laboratorio. Consulta tu resultado.");
        }
        if (sesion.getEstado() != EstadoSesion.EN_CURSO) {
            throw new ConflictException("La sesión ya terminó y no admite más respuestas");
        }

        MensajeSimulado mensaje = sesion.getEscenario().getMensajes().stream()
                .filter(m -> m.getId().equals(mensajeId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Mensaje", mensajeId));

        Set<TipoIndicador> indicadores = request.clasificacion() == Clasificacion.LEGITIMO ? Set.of() : request.indicadores();
        RespuestaMensaje respuesta = sesion.respuestaDe(mensajeId).orElseGet(() -> {
            RespuestaMensaje r = new RespuestaMensaje(mensaje);
            sesion.agregarRespuesta(r);
            return r;
        });
        respuesta.actualizar(request.clasificacion(), request.accion(), indicadores, ahora);
        sesionRepository.saveAndFlush(sesion);
        return RespuestaResponse.from(respuesta);
    }

    /** Termina la sesión y devuelve la retroalimentación. Repetir la llamada devuelve el mismo resultado. */
    @Transactional
    public ResultadoResponse finalizar(String sesionUuid, TokenInfo principal) {
        SesionSimulacion sesion = buscarPropia(sesionUuid, principal);
        LocalDateTime ahora = ahora();
        if (sesion.getEstado() == EstadoSesion.EN_CURSO) {
            cerrar(sesion, ahora);
        }
        return aResultado(sesion);
    }

    @Transactional
    public ResultadoResponse resultado(String sesionUuid, TokenInfo principal) {
        SesionSimulacion sesion = buscarLectura(sesionUuid, principal);
        cerrarSiVencio(sesion, ahora());
        if (sesion.getEstado() == EstadoSesion.EN_CURSO) {
            throw new ConflictException("La sesión sigue en curso: finalízala para ver el resultado");
        }
        return aResultado(sesion);
    }

    @Transactional(readOnly = true)
    public PageResponse<SesionResumen> misSesiones(TokenInfo principal, Pageable pageable) {
        return PageResponse.from(sesionRepository.findByUsuarioId(principal.usuarioId(), pageable)
                .map(SesionResumen::from));
    }

    // ------------------------------------------------------------------ internos

    /** Solo el dueño puede responder o finalizar. Para cualquier otro, la sesión no existe. */
    private SesionSimulacion buscarPropia(String uuid, TokenInfo principal) {
        return sesionRepository.findByUuid(uuid)
                .filter(s -> s.getUsuarioId().equals(principal.usuarioId()))
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión", uuid));
    }

    /** El dueño o un ADMIN pueden consultar. */
    private SesionSimulacion buscarLectura(String uuid, TokenInfo principal) {
        return sesionRepository.findByUuid(uuid)
                .filter(s -> s.getUsuarioId().equals(principal.usuarioId()) || EscenarioService.esAdmin(principal))
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión", uuid));
    }

    private boolean cerrarSiVencio(SesionSimulacion sesion, LocalDateTime ahora) {
        if (sesion.getEstado() == EstadoSesion.EN_CURSO && sesion.estaVencida(ahora)) {
            cerrar(sesion, ahora);
            return true;
        }
        return false;
    }

    private void cerrar(SesionSimulacion sesion, LocalDateTime ahora) {
        ResultadoCalificacion r = calificar(sesion);
        EstadoSesion estadoFinal = sesion.estaVencida(ahora) ? EstadoSesion.VENCIDA : EstadoSesion.FINALIZADA;
        sesion.cerrar(estadoFinal, ahora, r.puntaje(), r.puntajeMaximo());
        sesionRepository.saveAndFlush(sesion);
        log.info("Sesión de simulación cerrada: sesion={} estado={} puntaje={}/{}",
                sesion.getUuid(), estadoFinal, r.puntaje(), r.puntajeMaximo());
    }

    private ResultadoCalificacion calificar(SesionSimulacion sesion) {
        Map<Long, RespuestaMensaje> porMensaje = sesion.getRespuestas().stream()
                .collect(Collectors.toMap(r -> r.getMensaje().getId(), Function.identity()));
        return calificador.calificar(sesion.getEscenario().getMensajes(), porMensaje);
    }

    private SesionResponse aRespuesta(SesionSimulacion sesion, LocalDateTime ahora) {
        Escenario escenario = sesion.getEscenario();
        List<MensajeBandeja> bandeja = escenario.getMensajes().stream()
                .map(m -> MensajeBandeja.from(m, sesion.respuestaDe(m.getId()).orElse(null)))
                .toList();
        long restantes = sesion.getEstado() == EstadoSesion.EN_CURSO
                ? Math.max(0, Duration.between(ahora, sesion.getVenceEn()).toSeconds())
                : 0;
        return new SesionResponse(sesion.getUuid(), sesion.getEstado(), sesion.getIniciadaEn(), sesion.getVenceEn(),
                restantes, sesion.getRespuestas().size(), bandeja.size(), EscenarioDetalle.from(escenario), bandeja);
    }

    private ResultadoResponse aResultado(SesionSimulacion sesion) {
        ResultadoCalificacion r = calificar(sesion);
        List<RetroalimentacionMensaje> mensajes = r.evaluaciones().stream().map(this::aRetroalimentacion).toList();

        int totalPhishing = (int) r.evaluaciones().stream().filter(e -> e.mensaje().getEsPhishing()).count();
        int detectados = (int) r.evaluaciones().stream()
                .filter(e -> e.mensaje().getEsPhishing() && e.clasificacionCorrecta()).count();
        int falsosPositivos = (int) r.evaluaciones().stream()
                .filter(e -> !e.mensaje().getEsPhishing() && e.respondido() && !e.clasificacionCorrecta()).count();
        int peligrosas = (int) r.evaluaciones().stream().filter(EvaluacionMensaje::accionPeligrosa).count();

        Escenario escenario = sesion.getEscenario();
        return new ResultadoResponse(sesion.getUuid(), escenario.getUuid(), escenario.getTitulo(), sesion.getEstado(),
                sesion.getFinalizadaEn(), r.puntaje(), r.puntajeMaximo(), r.porcentaje(), r.aprobado(),
                sesion.getRespuestas().size(), r.evaluaciones().size(), detectados, totalPhishing, falsosPositivos,
                peligrosas, r.senalesAReforzar().stream().map(IndicadorCatalogo::from).toList(), mensajes);
    }

    private RetroalimentacionMensaje aRetroalimentacion(EvaluacionMensaje e) {
        MensajeSimulado m = e.mensaje();
        Map<TipoIndicador, Indicador> reales = m.getIndicadores().stream()
                .collect(Collectors.toMap(Indicador::getTipo, Function.identity()));
        RespuestaMensaje respuesta = e.respuesta();
        return new RetroalimentacionMensaje(
                m.getId(), m.getOrden(),
                m.getRemitenteNombre() + " <" + m.getRemitenteDireccion() + ">",
                m.getAsunto(), m.getEsPhishing(), e.respondido(),
                respuesta == null ? null : respuesta.getClasificacion(), e.clasificacionCorrecta(),
                respuesta == null ? null : respuesta.getAccion(), m.getAccionCorrecta(),
                e.accionCorrecta(), e.accionPeligrosa(),
                e.encontrados().stream().map(reales::get).map(SenalDetalle::from).toList(),
                e.omitidos().stream().map(reales::get).map(SenalDetalle::from).toList(),
                e.incorrectos().stream().map(IndicadorCatalogo::from).toList(),
                m.getExplicacion(), e.puntaje(), e.puntajeMaximo());
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
