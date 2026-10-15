package edusecurity.progreso.service;

import edusecurity.common.web.PageResponse;
import edusecurity.progreso.dto.SimulacionCompletadaRequest;
import edusecurity.progreso.dto.SimulacionCompletadaResponse;
import edusecurity.progreso.entity.SimulacionCompletada;
import edusecurity.progreso.repository.SimulacionCompletadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resultados de simulaciones terminadas, recibidos de simulacion-service (I7.4). */
@Service
public class SimulacionCompletadaService {

    private static final Logger log = LoggerFactory.getLogger(SimulacionCompletadaService.class);

    private final SimulacionCompletadaRepository repository;

    public SimulacionCompletadaService(SimulacionCompletadaRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra el resultado de una sesión. Es idempotente por {@code sesionUuid}: si ya existe,
     * devuelve el registro guardado sin modificarlo (simulacion-service reintenta los envíos).
     */
    public Registro registrar(SimulacionCompletadaRequest request) {
        var existente = repository.findBySesionUuid(request.sesionUuid());
        if (existente.isPresent()) {
            return new Registro(SimulacionCompletadaResponse.from(existente.get()), false);
        }
        try {
            SimulacionCompletada guardada = repository.saveAndFlush(nueva(request));
            log.info("Simulación registrada: sesion={} usuario={} puntaje={}/{}", guardada.getSesionUuid(),
                    guardada.getUsuarioId(), guardada.getPuntaje(), guardada.getPuntajeMaximo());
            return new Registro(SimulacionCompletadaResponse.from(guardada), true);
        } catch (DataIntegrityViolationException ex) {
            // Dos envíos simultáneos de la misma sesión: gana el primero.
            return repository.findBySesionUuid(request.sesionUuid())
                    .map(s -> new Registro(SimulacionCompletadaResponse.from(s), false))
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<SimulacionCompletadaResponse> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return PageResponse.from(repository.findByUsuarioId(usuarioId, pageable).map(SimulacionCompletadaResponse::from));
    }

    private static SimulacionCompletada nueva(SimulacionCompletadaRequest r) {
        SimulacionCompletada s = new SimulacionCompletada();
        s.setUsuarioId(r.usuarioId());
        s.setSesionUuid(r.sesionUuid());
        s.setEscenarioUuid(r.escenarioUuid());
        s.setTipo(r.tipo());
        s.setTitulo(r.titulo());
        s.setPuntaje(r.puntaje());
        s.setPuntajeMaximo(r.puntajeMaximo());
        s.setPorcentaje(r.porcentaje());
        s.setAprobado(r.aprobado());
        s.setEstado(r.estado());
        s.setFinalizadaEn(r.finalizadaEn());
        return s;
    }

    /** @param creado true si el registro se creó en esta llamada; false si ya existía. */
    public record Registro(SimulacionCompletadaResponse simulacion, boolean creado) {
    }
}
