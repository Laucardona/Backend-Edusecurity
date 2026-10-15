package edusecurity.contenido.service;

import edusecurity.contenido.dto.EstadoRequest;
import edusecurity.contenido.dto.LeccionResponse;
import edusecurity.contenido.dto.ModuloRequest;
import edusecurity.contenido.dto.ModuloResponse;
import edusecurity.contenido.entity.Modulo;
import edusecurity.contenido.repository.LeccionRepository;
import edusecurity.contenido.repository.ModuloRepository;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.web.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuloService {

    private static final Logger log = LoggerFactory.getLogger(ModuloService.class);

    private final ModuloRepository moduloRepository;
    private final LeccionRepository leccionRepository;

    public ModuloService(ModuloRepository moduloRepository, LeccionRepository leccionRepository) {
        this.moduloRepository = moduloRepository;
        this.leccionRepository = leccionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ModuloResponse> listar(String q, boolean soloActivos, Pageable pageable) {
        String consulta = (q == null || q.isBlank()) ? null : q.trim();
        Page<Modulo> page = moduloRepository.buscar(consulta, soloActivos, pageable);

        Map<Long, Long> conteos = contarLecciones(
                page.getContent().stream().map(Modulo::getId).toList());

        return PageResponse.from(page.map(modulo ->
                ModuloResponse.from(modulo, conteos.getOrDefault(modulo.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public ModuloResponse obtener(Long id) {
        Modulo modulo = buscar(id);
        long total = leccionRepository.countByModuloId(id);
        return ModuloResponse.from(modulo, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeccionResponse> listarLecciones(Long moduloId, Pageable pageable) {
        buscar(moduloId);
        Page<LeccionResponse> page =
                leccionRepository.findByModuloId(moduloId, pageable).map(LeccionResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ModuloResponse crear(ModuloRequest request) {
        Modulo modulo = new Modulo();
        aplicar(modulo, request);
        modulo.setActivo(request.activo() == null || request.activo());

        Modulo guardado = moduloRepository.save(modulo);
        log.info("Módulo creado: id={} '{}'", guardado.getId(), guardado.getTitulo());
        return ModuloResponse.from(guardado, 0L);
    }

    @Transactional
    public ModuloResponse actualizar(Long id, ModuloRequest request) {
        Modulo modulo = buscar(id);
        aplicar(modulo, request);

        Modulo guardado = moduloRepository.save(modulo);
        return ModuloResponse.from(guardado, leccionRepository.countByModuloId(id));
    }

    @Transactional
    public ModuloResponse cambiarEstado(Long id, EstadoRequest request) {
        Modulo modulo = buscar(id);
        modulo.setActivo(request.activo());
        Modulo guardado = moduloRepository.save(modulo);
        return ModuloResponse.from(guardado, leccionRepository.countByModuloId(id));
    }

    /**
     * Elimina un módulo. Si tiene lecciones se debe indicar {@code force=true}
     * para eliminarlas en cascada.
     */
    @Transactional
    public void eliminar(Long id, boolean force) {
        Modulo modulo = buscar(id);
        long lecciones = leccionRepository.countByModuloId(id);

        if (lecciones > 0 && !force) {
            throw new ConflictException(
                    "El módulo tiene " + lecciones + " lección(es). Use force=true para eliminarlas junto al módulo");
        }

        if (lecciones > 0) {
            leccionRepository.eliminarPorModulo(id);
        }

        moduloRepository.delete(modulo);
        log.info("Módulo eliminado: id={} (lecciones eliminadas={})", id, lecciones);
    }

    private void aplicar(Modulo modulo, ModuloRequest request) {
        modulo.setTitulo(request.titulo().trim());
        modulo.setDescripcion(request.descripcion());
        modulo.setOrden(request.orden());
        if (request.activo() != null) {
            modulo.setActivo(request.activo());
        }
    }

    private Map<Long, Long> contarLecciones(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return moduloRepository.contarLeccionesPorModulo(ids).stream()
                .collect(Collectors.toMap(
                        fila -> (Long) fila[0],
                        fila -> (Long) fila[1],
                        (a, b) -> a));
    }

    public Modulo buscar(Long id) {
        return moduloRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Módulo", id));
    }
}
