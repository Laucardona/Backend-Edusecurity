package edusecurity.simulacion.service;

import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import edusecurity.simulacion.dto.AccionRequest;
import edusecurity.simulacion.dto.AccionResponse;
import edusecurity.simulacion.dto.EscenarioDetalle;
import edusecurity.simulacion.dto.IndicadorCatalogo;
import edusecurity.simulacion.dto.MensajeBandeja;
import edusecurity.simulacion.dto.PasoRecorrido;
import edusecurity.simulacion.dto.PasoResponse;
import edusecurity.simulacion.dto.RespuestaRequest;
import edusecurity.simulacion.dto.RespuestaResponse;
import edusecurity.simulacion.dto.ResultadoResponse;
import edusecurity.simulacion.dto.RetroalimentacionMensaje;
import edusecurity.simulacion.dto.SenalDetalle;
import edusecurity.simulacion.dto.SesionResponse;
import edusecurity.simulacion.dto.SesionResumen;
import edusecurity.simulacion.entity.AccionLaboratorio;
import edusecurity.simulacion.entity.Clasificacion;
import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.EstadoSesion;
import edusecurity.simulacion.entity.Indicador;
import edusecurity.simulacion.entity.MensajeSimulado;
import edusecurity.simulacion.entity.OpcionPaso;
import edusecurity.simulacion.entity.PasoLaboratorio;
import edusecurity.simulacion.entity.RespuestaMensaje;
import edusecurity.simulacion.entity.SesionSimulacion;
import edusecurity.simulacion.entity.TipoIndicador;
import edusecurity.simulacion.repository.SesionSimulacionRepository;
import edusecurity.simulacion.service.CalificadorSimulacion.EvaluacionMensaje;
import edusecurity.simulacion.service.CalificadorSimulacion.ResultadoCalificacion;
import edusecurity.simulacion.service.MaquinaLaboratorio.Calificacion;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de una sesión: iniciar, responder (mensajes de la bandeja en phishing o decisiones paso a
 * paso en un laboratorio guiado), finalizar y ver la retroalimentación.
 * El tiempo límite se controla en el servidor; el reloj del cliente no cuenta.
 */
@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    private final SesionSimulacionRepository sesionRepository;
    private final EscenarioService escenarioService;
    private final CalificadorSimulacion calificador;
    private final MaquinaLaboratorio maquina;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    public SesionService(SesionSimulacionRepository sesionRepository, EscenarioService escenarioService,
                         CalificadorSimulacion calificador, MaquinaLaboratorio maquina, Clock clock,
                         ApplicationEventPublisher eventos) {
        this.sesionRepository = sesionRepository;
        this.escenarioService = escenarioService;
        this.calificador = calificador;
        this.maquina = maquina;
        this.clock = clock;
        this.eventos = eventos;
    }

    /**
     * Inicia el laboratorio. Si el estudiante ya tiene una sesión en curso de este escenario,
     * la devuelve en lugar de crear otra (recargar la página no reinicia el reloj).
     * En un laboratorio guiado, la sesión empieza en el paso de menor orden.
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

        SesionSimulacion nueva = new SesionSimulacion(escenario, principal.usuarioId(), ahora);
        if (escenario.esLaboratorioGuiado()) {
            nueva.setPasoActual(maquina.pasoInicial(escenario)
                    .orElseThrow(() -> new ConflictException("El laboratorio no tiene pasos configurados")));
        }
        nueva = sesionRepository.save(nueva);
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

    /** Guarda o cambia la decisión sobre un mensaje mientras la sesión siga abierta. Solo phishing. */
    @Transactional(noRollbackFor = ConflictException.class)
    public RespuestaResponse responder(String sesionUuid, Long mensajeId, RespuestaRequest request, TokenInfo principal) {
        SesionSimulacion sesion = buscarPropia(sesionUuid, principal);
        if (sesion.getEscenario().esLaboratorioGuiado()) {
            throw new BadRequestException("Esta sesión es un laboratorio guiado: responde con /acciones, no con /respuestas");
        }
        LocalDateTime ahora = ahora();
        validarAbierta(sesion, ahora);

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

    /** Paso actual del laboratorio guiado, sin puntos, respuesta correcta ni retroalimentación. */
    @Transactional(noRollbackFor = ConflictException.class)
    public PasoResponse pasoActual(String sesionUuid, TokenInfo principal) {
        SesionSimulacion sesion = buscarLectura(sesionUuid, principal);
        exigirLaboratorio(sesion);
        validarAbierta(sesion, ahora());
        return PasoResponse.from(sesion.getPasoActual());
    }

    /**
     * Registra la decisión del estudiante en el paso actual y avanza la máquina de estados.
     * Si la opción no lleva a otro paso, o el paso es final, la sesión se cierra con su puntaje.
     */
    @Transactional(noRollbackFor = ConflictException.class)
    public AccionResponse registrarAccion(String sesionUuid, AccionRequest request, TokenInfo principal) {
        SesionSimulacion sesion = buscarPropia(sesionUuid, principal);
        exigirLaboratorio(sesion);
        LocalDateTime ahora = ahora();
        validarAbierta(sesion, ahora);

        PasoLaboratorio paso = sesion.getPasoActual();
        if (!paso.getCodigo().equals(request.pasoCodigo())) {
            throw new ConflictException("El paso '" + request.pasoCodigo() + "' no es el paso actual del laboratorio ('"
                    + paso.getCodigo() + "')");
        }
        if (sesion.pasoRespondido(paso.getId())) {
            throw new ConflictException("El paso '" + paso.getCodigo() + "' ya fue respondido");
        }
        OpcionPaso opcion = paso.opcion(request.opcionCodigo())
                .orElseThrow(() -> new BadRequestException("La opción '" + request.opcionCodigo()
                        + "' no pertenece al paso '" + paso.getCodigo() + "'"));

        sesion.agregarAccion(new AccionLaboratorio(paso, opcion, ahora));
        Optional<PasoLaboratorio> siguiente = maquina.siguiente(paso, opcion);
        sesion.setPasoActual(siguiente.orElse(null));
        if (siguiente.isEmpty()) {
            cerrar(sesion, ahora);
        } else {
            sesionRepository.saveAndFlush(sesion);
        }

        int acumulado = sesion.getAcciones().stream().mapToInt(AccionLaboratorio::getPuntos).sum();
        return new AccionResponse(paso.getCodigo(), opcion.getCodigo(), Boolean.TRUE.equals(opcion.getEsCorrecta()),
                opcion.getPuntos(), opcion.getRetroalimentacion(), acumulado, siguiente.isEmpty(),
                siguiente.map(PasoResponse::from).orElse(null));
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

    private void exigirLaboratorio(SesionSimulacion sesion) {
        if (!sesion.getEscenario().esLaboratorioGuiado()) {
            throw new BadRequestException("Esta sesión no es un laboratorio guiado: responde los mensajes con /respuestas");
        }
    }

    /** Cierra la sesión si venció. En cualquier caso, si ya no está en curso no admite más respuestas. */
    private void validarAbierta(SesionSimulacion sesion, LocalDateTime ahora) {
        if (cerrarSiVencio(sesion, ahora)) {
            throw new ConflictException("Se agotó el tiempo del laboratorio. Consulta tu resultado.");
        }
        if (sesion.getEstado() != EstadoSesion.EN_CURSO) {
            throw new ConflictException("La sesión ya terminó y no admite más respuestas");
        }
    }

    private boolean cerrarSiVencio(SesionSimulacion sesion, LocalDateTime ahora) {
        if (sesion.getEstado() == EstadoSesion.EN_CURSO && sesion.estaVencida(ahora)) {
            cerrar(sesion, ahora);
            return true;
        }
        return false;
    }

    private void cerrar(SesionSimulacion sesion, LocalDateTime ahora) {
        int puntaje;
        int maximo;
        if (sesion.getEscenario().esLaboratorioGuiado()) {
            Calificacion c = maquina.calificar(sesion.getEscenario(), sesion.getAcciones());
            puntaje = c.puntaje();
            maximo = c.puntajeMaximo();
            sesion.setPasoActual(null);
        } else {
            ResultadoCalificacion r = calificar(sesion);
            puntaje = r.puntaje();
            maximo = r.puntajeMaximo();
        }
        EstadoSesion estadoFinal = sesion.estaVencida(ahora) ? EstadoSesion.VENCIDA : EstadoSesion.FINALIZADA;
        sesion.cerrar(estadoFinal, ahora, puntaje, maximo);
        sesionRepository.saveAndFlush(sesion);
        log.info("Sesión de simulación cerrada: sesion={} estado={} puntaje={}/{}",
                sesion.getUuid(), estadoFinal, puntaje, maximo);
        // NotificadorProgreso lo envía a progreso-service después del commit (I7.4).
        eventos.publishEvent(new SesionCerradaEvent(sesion.getId()));
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
        PasoResponse paso = sesion.getEstado() == EstadoSesion.EN_CURSO && sesion.getPasoActual() != null
                ? PasoResponse.from(sesion.getPasoActual())
                : null;
        return new SesionResponse(sesion.getUuid(), sesion.getEstado(), sesion.getIniciadaEn(), sesion.getVenceEn(),
                restantes, sesion.getRespuestas().size(), bandeja.size(), EscenarioDetalle.from(escenario), bandeja,
                sesion.getAcciones().size(), paso);
    }

    private ResultadoResponse aResultado(SesionSimulacion sesion) {
        if (sesion.getEscenario().esLaboratorioGuiado()) {
            return aResultadoLaboratorio(sesion);
        }
        ResultadoCalificacion r = calificar(sesion);
        List<RetroalimentacionMensaje> mensajes = r.evaluaciones().stream().map(this::aRetroalimentacion).toList();

        int totalPhishing = (int) r.evaluaciones().stream().filter(e -> e.mensaje().getEsPhishing()).count();
        int detectados = (int) r.evaluaciones().stream()
                .filter(e -> e.mensaje().getEsPhishing() && e.clasificacionCorrecta()).count();
        int falsosPositivos = (int) r.evaluaciones().stream()
                .filter(e -> !e.mensaje().getEsPhishing() && e.respondido() && !e.clasificacionCorrecta()).count();
        int peligrosas = (int) r.evaluaciones().stream().filter(EvaluacionMensaje::accionPeligrosa).count();

        Escenario escenario = sesion.getEscenario();
        return new ResultadoResponse(sesion.getUuid(), escenario.getUuid(), escenario.getTitulo(), escenario.getTipo(),
                sesion.getEstado(), sesion.getFinalizadaEn(), r.puntaje(), r.puntajeMaximo(), r.porcentaje(),
                r.aprobado(), sesion.getRespuestas().size(), r.evaluaciones().size(), detectados, totalPhishing,
                falsosPositivos, peligrosas, r.senalesAReforzar().stream().map(IndicadorCatalogo::from).toList(),
                mensajes, List.of());
    }

    private ResultadoResponse aResultadoLaboratorio(SesionSimulacion sesion) {
        Escenario escenario = sesion.getEscenario();
        Calificacion c = maquina.calificar(escenario, sesion.getAcciones());

        List<PasoRecorrido> recorrido = new ArrayList<>();
        int secuencia = 1;
        for (AccionLaboratorio accion : sesion.getAcciones()) {
            PasoLaboratorio paso = accion.getPaso();
            OpcionPaso elegida = accion.getOpcion();
            OpcionPaso mejor = maquina.mejorOpcion(paso);
            recorrido.add(new PasoRecorrido(secuencia++, paso.getCodigo(), paso.getTitulo(),
                    elegida.getCodigo(), elegida.getTexto(), Boolean.TRUE.equals(elegida.getEsCorrecta()),
                    accion.getPuntos(), elegida.getRetroalimentacion(),
                    mejor.getCodigo(), mejor.getTexto(), mejor.getPuntos(), mejor.getRetroalimentacion()));
        }

        return new ResultadoResponse(sesion.getUuid(), escenario.getUuid(), escenario.getTitulo(), escenario.getTipo(),
                sesion.getEstado(), sesion.getFinalizadaEn(), c.puntaje(), c.puntajeMaximo(), c.porcentaje(),
                c.aprobado(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), recorrido);
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
