package edusecurity.progreso.service;

import edusecurity.progreso.client.LeccionFeign;
import edusecurity.progreso.dto.ResumenGlobalResponse;
import edusecurity.progreso.dto.ResumenModuloGlobalResponse;
import edusecurity.progreso.dto.ResumenModuloResponse;
import edusecurity.progreso.dto.ResumenUsuarioResponse;
import edusecurity.progreso.entity.Progreso;
import edusecurity.progreso.repository.ProgresoRepository;
import edusecurity.progreso.repository.SimulacionCompletadaRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estadísticas de progreso: resumen por usuario y resumen global.
 * El catálogo de lecciones se obtiene del servicio de contenido vía Feign.
 */
@Service
public class ResumenService {

    private final ProgresoService progresoService;
    private final ProgresoRepository repository;
    private final SimulacionCompletadaRepository simulacionRepository;

    public ResumenService(ProgresoService progresoService, ProgresoRepository repository,
                          SimulacionCompletadaRepository simulacionRepository) {
        this.progresoService = progresoService;
        this.repository = repository;
        this.simulacionRepository = simulacionRepository;
    }

    @Transactional(readOnly = true)
    public ResumenUsuarioResponse resumenUsuario(Long usuarioId) {
        List<Progreso> progresos = repository.findByUsuarioId(usuarioId);
        List<LeccionFeign> lecciones = progresoService.todasLasLecciones();

        long totalLecciones = lecciones.size();
        long completadas = progresos.stream().filter(Progreso::getCompletado).count();
        Double promedio = promedio(progresos.stream().map(Progreso::getPorcentaje).toList());

        Map<Long, LeccionFeign> leccionesPorId = lecciones.stream()
                .collect(Collectors.toMap(LeccionFeign::id, Function.identity(), (a, b) -> a));

        Set<Long> moduloIds = progresos.stream()
                .map(Progreso::getModuloId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ResumenModuloResponse> porModulo = moduloIds.stream()
                .map(moduloId -> resumenModuloDeUsuario(moduloId, progresos, leccionesPorId))
                .sorted(Comparator.comparing(ResumenModuloResponse::moduloId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        LocalDateTime ultimaActividad = progresos.stream()
                .map(Progreso::getFechaActualizacion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new ResumenUsuarioResponse(
                usuarioId,
                progresoService.nombreUsuario(usuarioId),
                totalLecciones,
                progresos.size(),
                completadas,
                promedio,
                ultimaActividad,
                porModulo,
                simulacionRepository.countByUsuarioId(usuarioId),
                simulacionRepository.countByUsuarioIdAndAprobadoTrue(usuarioId));
    }

    @Transactional(readOnly = true)
    public ResumenGlobalResponse resumenGlobal() {
        List<Progreso> todos = repository.findAll();
        List<LeccionFeign> lecciones = progresoService.todasLasLecciones();

        Map<Long, LeccionFeign> leccionesPorId = lecciones.stream()
                .collect(Collectors.toMap(LeccionFeign::id, Function.identity(), (a, b) -> a));

        Set<Long> moduloIds = lecciones.stream()
                .map(leccion -> (leccion.modulo() == null) ? null : leccion.modulo().id())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        todos.forEach(p -> {
            if (p.getModuloId() != null) {
                moduloIds.add(p.getModuloId());
            }
        });

        Map<Long, Long> totalesPorModulo = lecciones.stream()
                .filter(leccion -> leccion.modulo() != null)
                .collect(Collectors.groupingBy(leccion -> leccion.modulo().id(),
                        Collectors.counting()));

        List<ResumenModuloGlobalResponse> porModulo = moduloIds.stream()
                .map(moduloId -> {
                    List<Progreso> delModulo = todos.stream()
                            .filter(p -> moduloId.equals(p.getModuloId()))
                            .toList();
                    String titulo = tituloModulo(moduloId, leccionesPorId);
                    long completados = delModulo.stream().filter(Progreso::getCompletado).count();
                    Double prom = promedio(delModulo.stream()
                            .map(Progreso::getPorcentaje).toList());
                    return new ResumenModuloGlobalResponse(
                            moduloId,
                            titulo,
                            totalesPorModulo.getOrDefault(moduloId, 0L),
                            delModulo.size(),
                            completados,
                            prom);
                })
                .sorted(Comparator.comparing(ResumenModuloGlobalResponse::moduloId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        long totalUsuarios = todos.stream()
                .map(Progreso::getUsuarioId)
                .collect(Collectors.toSet())
                .size();
        long completados = todos.stream().filter(Progreso::getCompletado).count();

        return new ResumenGlobalResponse(
                todos.size(),
                totalUsuarios,
                completados,
                promedio(todos.stream().map(Progreso::getPorcentaje).toList()),
                porModulo);
    }

    /** Reexportado para que el controlador pueda paginar sin duplicar lógica. */
    private ResumenModuloResponse resumenModuloDeUsuario(Long moduloId, List<Progreso> progresos,
                                                         Map<Long, LeccionFeign> leccionesPorId) {
        List<Progreso> delModulo = progresos.stream()
                .filter(p -> moduloId.equals(p.getModuloId()))
                .toList();

        long totalLecciones = leccionesPorId.values().stream()
                .filter(leccion -> leccion.modulo() != null && moduloId.equals(leccion.modulo().id()))
                .count();

        long completadas = delModulo.stream().filter(Progreso::getCompletado).count();
        String titulo = tituloModulo(moduloId, leccionesPorId);

        return new ResumenModuloResponse(moduloId, titulo, totalLecciones, completadas,
                promedio(delModulo.stream().map(Progreso::getPorcentaje).toList()));
    }

    private String tituloModulo(Long moduloId, Map<Long, LeccionFeign> leccionesPorId) {
        return leccionesPorId.values().stream()
                .filter(leccion -> leccion.modulo() != null && moduloId.equals(leccion.modulo().id()))
                .map(leccion -> leccion.modulo().titulo())
                .findFirst()
                .orElse("Módulo #" + moduloId);
    }

    private Double promedio(List<Double> valores) {
        if (valores.isEmpty()) {
            return 0.0;
        }
        return Math.round((valores.stream().mapToDouble(Double::doubleValue).average().orElse(0)) * 100)
                / 100.0;
    }
}
