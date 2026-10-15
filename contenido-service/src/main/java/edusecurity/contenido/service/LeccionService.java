package edusecurity.contenido.service;

import edusecurity.contenido.dto.LeccionRequest;
import edusecurity.contenido.dto.LeccionResponse;
import edusecurity.contenido.entity.Leccion;
import edusecurity.contenido.entity.Modulo;
import edusecurity.contenido.repository.LeccionRepository;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.web.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeccionService {

    private static final Logger log = LoggerFactory.getLogger(LeccionService.class);

    private final LeccionRepository leccionRepository;
    private final ModuloService moduloService;

    public LeccionService(LeccionRepository leccionRepository, ModuloService moduloService) {
        this.leccionRepository = leccionRepository;
        this.moduloService = moduloService;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeccionResponse> listar(String q, Long moduloId, Pageable pageable) {
        String consulta = (q == null || q.isBlank()) ? null : q.trim();
        Page<LeccionResponse> page =
                leccionRepository.buscar(consulta, moduloId, pageable).map(LeccionResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public LeccionResponse obtener(Long id) {
        return LeccionResponse.from(buscar(id));
    }

    @Transactional
    public LeccionResponse crear(LeccionRequest request) {
        Modulo modulo = resolverModulo(request.moduloId());

        Leccion leccion = new Leccion();
        aplicar(leccion, request, modulo);

        Leccion guardada = leccionRepository.save(leccion);
        log.info("Lección creada: id={} '{}'", guardada.getId(), guardada.getTitulo());
        return LeccionResponse.from(guardada);
    }

    @Transactional
    public LeccionResponse actualizar(Long id, LeccionRequest request) {
        Leccion leccion = buscar(id);
        Modulo modulo = resolverModulo(request.moduloId());
        aplicar(leccion, request, modulo);

        Leccion guardada = leccionRepository.save(leccion);
        return LeccionResponse.from(guardada);
    }

    @Transactional
    public void eliminar(Long id) {
        Leccion leccion = buscar(id);
        leccionRepository.delete(leccion);
        log.info("Lección eliminada: id={}", id);
    }

    private void aplicar(Leccion leccion, LeccionRequest request, Modulo modulo) {
        leccion.setTitulo(request.titulo().trim());
        leccion.setDescripcion(request.descripcion());
        leccion.setContenido(request.contenido());
        leccion.setOrden(request.orden());
        leccion.setActivo(request.activo() == null || request.activo());
        leccion.setModulo(modulo);
    }

    private Modulo resolverModulo(Long moduloId) {
        Modulo modulo = moduloService.buscar(moduloId);
        if (!Boolean.TRUE.equals(modulo.getActivo())) {
            throw new BadRequestException("No se pueden asignar lecciones a un módulo inactivo");
        }
        return modulo;
    }

    private Leccion buscar(Long id) {
        return leccionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lección", id));
    }
}
