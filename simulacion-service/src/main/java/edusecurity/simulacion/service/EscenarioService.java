package edusecurity.simulacion.service;

import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import edusecurity.simulacion.dto.EscenarioDetalle;
import edusecurity.simulacion.dto.EscenarioRequest;
import edusecurity.simulacion.dto.EscenarioResumen;
import edusecurity.simulacion.dto.IndicadorCatalogo;
import edusecurity.simulacion.dto.IndicadorRequest;
import edusecurity.simulacion.dto.MensajeRequest;
import edusecurity.simulacion.dto.PublicacionRequest;
import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.Indicador;
import edusecurity.simulacion.entity.MensajeSimulado;
import edusecurity.simulacion.entity.Nivel;
import edusecurity.simulacion.entity.TipoEscenario;
import edusecurity.simulacion.entity.TipoIndicador;
import edusecurity.simulacion.repository.EscenarioRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo de escenarios y su administración. */
@Service
public class EscenarioService {

    private static final Logger log = LoggerFactory.getLogger(EscenarioService.class);

    private final EscenarioRepository escenarioRepository;

    public EscenarioService(EscenarioRepository escenarioRepository) {
        this.escenarioRepository = escenarioRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EscenarioResumen> listar(TipoEscenario tipo, Nivel nivel, TokenInfo principal, Pageable pageable) {
        Page<Escenario> page = escenarioRepository.buscar(tipo, nivel, !esAdmin(principal), pageable);
        Map<Long, Long> conteos = contarMensajes(page.getContent().stream().map(Escenario::getId).toList());
        return PageResponse.from(page.map(e -> EscenarioResumen.from(e, conteos.getOrDefault(e.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public EscenarioDetalle obtener(String uuid, TokenInfo principal) {
        return EscenarioDetalle.from(buscarVisible(uuid, principal));
    }

    public List<IndicadorCatalogo> catalogoIndicadores() {
        return Arrays.stream(TipoIndicador.values()).map(IndicadorCatalogo::from).toList();
    }

    @Transactional
    public EscenarioDetalle crear(EscenarioRequest request) {
        Escenario escenario = new Escenario();
        escenario.setTipo(request.tipo());
        escenario.setTitulo(request.titulo().trim());
        escenario.setDescripcion(request.descripcion());
        escenario.setContexto(request.contexto().trim());
        escenario.setNivel(request.nivel());
        escenario.setDuracionSegundos(request.duracionSegundos());
        escenario.setPublicado(false);

        int orden = 1;
        for (MensajeRequest m : request.mensajes()) {
            escenario.agregarMensaje(construirMensaje(m, orden++));
        }

        Escenario guardado = escenarioRepository.save(escenario);
        log.info("Escenario creado: uuid={} '{}' con {} mensajes", guardado.getUuid(),
                guardado.getTitulo(), guardado.getMensajes().size());
        return EscenarioDetalle.from(guardado);
    }

    @Transactional
    public EscenarioDetalle cambiarPublicacion(String uuid, PublicacionRequest request) {
        Escenario escenario = escenarioRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("Escenario", uuid));
        escenario.setPublicado(request.publicado());
        return EscenarioDetalle.from(escenarioRepository.save(escenario));
    }

    /** Un escenario sin publicar solo lo ve el ADMIN; para el resto no existe. */
    Escenario buscarVisible(String uuid, TokenInfo principal) {
        return escenarioRepository.findByUuid(uuid)
                .filter(e -> Boolean.TRUE.equals(e.getPublicado()) || esAdmin(principal))
                .orElseThrow(() -> ResourceNotFoundException.of("Escenario", uuid));
    }

    private MensajeSimulado construirMensaje(MensajeRequest m, int orden) {
        List<IndicadorRequest> indicadores = m.indicadores() == null ? List.of() : m.indicadores();
        validarCoherencia(m, indicadores, orden);

        MensajeSimulado mensaje = new MensajeSimulado();
        mensaje.setOrden(orden);
        mensaje.setCanal(m.canal());
        mensaje.setRemitenteNombre(m.remitenteNombre().trim());
        mensaje.setRemitenteDireccion(m.remitenteDireccion().trim());
        mensaje.setAsunto(m.asunto());
        mensaje.setCuerpo(m.cuerpo());
        mensaje.setEnlaceMostrado(m.enlaceMostrado());
        mensaje.setEnlaceReal(m.enlaceReal());
        mensaje.setAdjuntoNombre(m.adjuntoNombre());
        mensaje.setRecibidoEn(m.recibidoEn());
        mensaje.setEsPhishing(m.esPhishing());
        mensaje.setAccionCorrecta(m.accionCorrecta());
        mensaje.setExplicacion(m.explicacion());
        indicadores.forEach(i -> mensaje.agregarIndicador(new Indicador(i.tipo(), i.descripcion(), i.fragmento())));
        return mensaje;
    }

    /**
     * Evita escenarios que enseñen mal: un phishing debe tener señales y una acción segura,
     * y un mensaje legítimo no puede tener señales de alerta.
     */
    private void validarCoherencia(MensajeRequest m, List<IndicadorRequest> indicadores, int orden) {
        String prefijo = "Mensaje " + orden + ": ";
        if (m.esPhishing()) {
            if (indicadores.isEmpty()) {
                throw new BadRequestException(prefijo + "un mensaje de phishing debe tener al menos una señal de alerta");
            }
            if (!m.accionCorrecta().esSeguraAnteAtaque()) {
                throw new BadRequestException(prefijo + "la acción correcta ante phishing debe ser REPORTAR, ELIMINAR o VERIFICAR_POR_OTRO_CANAL");
            }
        } else {
            if (!indicadores.isEmpty()) {
                throw new BadRequestException(prefijo + "un mensaje legítimo no debe tener señales de alerta");
            }
            if (m.accionCorrecta().esPeligrosaAnteAtaque()) {
                throw new BadRequestException(prefijo + "la acción recomendada no puede ser RESPONDER ni ABRIR_ENLACE");
            }
        }
        Set<TipoIndicador> vistos = new HashSet<>();
        for (IndicadorRequest i : indicadores) {
            if (!vistos.add(i.tipo())) {
                throw new BadRequestException(prefijo + "la señal " + i.tipo() + " está repetida");
            }
        }
    }

    private Map<Long, Long> contarMensajes(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return escenarioRepository.contarMensajes(ids).stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> (Long) f[1], (a, b) -> a));
    }

    static boolean esAdmin(TokenInfo principal) {
        return principal != null && "ADMIN".equals(principal.rol());
    }
}
