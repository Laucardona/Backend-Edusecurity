package edusecurity.progreso.service;

import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ForbiddenException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.exception.ServiceUnavailableException;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import edusecurity.progreso.client.AuthClient;
import edusecurity.progreso.client.ContenidoClient;
import edusecurity.progreso.client.LeccionFeign;
import edusecurity.progreso.client.UsuarioFeign;
import edusecurity.progreso.dto.ProgresoRequest;
import edusecurity.progreso.dto.ProgresoResponse;
import edusecurity.progreso.entity.Progreso;
import edusecurity.progreso.repository.ProgresoRepository;
import feign.FeignException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgresoService {

    private static final Logger log = LoggerFactory.getLogger(ProgresoService.class);

    private final ProgresoRepository repository;
    private final AuthClient authClient;
    private final ContenidoClient contenidoClient;

    public ProgresoService(ProgresoRepository repository, AuthClient authClient,
                           ContenidoClient contenidoClient) {
        this.repository = repository;
        this.authClient = authClient;
        this.contenidoClient = contenidoClient;
    }

    /**
     * Registra o actualiza el progreso (upsert por usuario + lección).
     */
    @Transactional
    public ProgresoResponse guardar(ProgresoRequest request, TokenInfo principal) {
        Long usuarioId = resolverUsuarioId(request, principal);
        LeccionFeign leccion = obtenerLeccion(request.leccionId());

        if (!Boolean.TRUE.equals(leccion.activo())) {
            throw new BadRequestException("La lección " + leccion.id() + " está inactiva");
        }
        if (leccion.modulo() == null || leccion.modulo().id() == null) {
            throw new BadRequestException("La lección no pertenece a un módulo válido");
        }

        boolean completado = Boolean.TRUE.equals(request.completado());
        double porcentaje = completado ? 100.0 : request.porcentaje();

        Progreso progreso = repository.findByUsuarioIdAndLeccionId(usuarioId, leccion.id())
                .orElseGet(() -> {
                    Progreso nuevo = new Progreso();
                    nuevo.setUsuarioId(usuarioId);
                    nuevo.setLeccionId(leccion.id());
                    return nuevo;
                });

        progreso.setModuloId(leccion.modulo().id());
        progreso.setPorcentaje(porcentaje);
        progreso.setCompletado(completado);

        Progreso guardado = repository.save(progreso);
        log.debug("Progreso guardado: usuario={} leccion={} porcentaje={}",
                usuarioId, leccion.id(), porcentaje);

        return ProgresoResponse.of(guardado,
                nombreUsuario(usuarioId),
                leccion.titulo(),
                leccion.modulo().titulo());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProgresoResponse> listar(Long usuarioId, Long leccionId, Long moduloId,
                                                 Pageable pageable) {
        Page<Progreso> page = repository.buscar(usuarioId, leccionId, moduloId, pageable);
        return PageResponse.from(page.map(this::aResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProgresoResponse> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return PageResponse.from(
                repository.buscar(usuarioId, null, null, pageable).map(this::aResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProgresoResponse> listarPorLeccion(Long leccionId, Pageable pageable) {
        return PageResponse.from(
                repository.buscar(null, leccionId, null, pageable).map(this::aResponse));
    }

    @Transactional(readOnly = true)
    public ProgresoResponse obtener(Long id, TokenInfo principal) {
        Progreso progreso = buscar(id);
        validarPropietario(progreso.getUsuarioId(), principal);
        return aResponse(progreso);
    }

    @Transactional
    public void eliminar(Long id) {
        Progreso progreso = buscar(id);
        repository.delete(progreso);
        log.info("Progreso eliminado: id={}", id);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Long resolverUsuarioId(ProgresoRequest request, TokenInfo principal) {
        Long solicitado = (request.usuarioId() != null) ? request.usuarioId() : principal.usuarioId();

        if (solicitado == null) {
            throw new BadRequestException("Debe indicar el usuario (usuarioId)");
        }

        boolean esPropio = solicitado.equals(principal.usuarioId());
        boolean esAdmin = "ADMIN".equals(principal.rol());

        if (!esPropio && !esAdmin) {
            throw new ForbiddenException("Solo puede registrar su propio progreso");
        }

        return solicitado;
    }

    private void validarPropietario(Long duenio, TokenInfo principal) {
        boolean esAdmin = "ADMIN".equals(principal.rol()) || "INTERNAL".equals(principal.rol());
        boolean esPropio = duenio.equals(principal.usuarioId());

        if (!esPropio && !esAdmin) {
            throw new ForbiddenException("No tiene acceso a este progreso");
        }
    }

    private Progreso buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Progreso", id));
    }

    private ProgresoResponse aResponse(Progreso progreso) {
        LeccionFeign leccion = intentarLeccion(progreso.getLeccionId());
        String tituloLeccion = (leccion == null)
                ? "Lección #" + progreso.getLeccionId()
                : leccion.titulo();
        String tituloModulo = (leccion == null || leccion.modulo() == null)
                ? "Módulo #" + progreso.getModuloId()
                : leccion.modulo().titulo();

        return ProgresoResponse.of(progreso,
                nombreUsuario(progreso.getUsuarioId()),
                tituloLeccion,
                tituloModulo);
    }

    /** Resuelve una lección vía Feign distinguiendo "no existe" de "servicio caído". */
    protected LeccionFeign obtenerLeccion(Long leccionId) {
        LeccionFeign leccion;
        try {
            leccion = contenidoClient.obtenerLeccion(leccionId);
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw ResourceNotFoundException.of("Lección", leccionId);
            }
            throw new ServiceUnavailableException("El servicio de contenido no está disponible", ex);
        } catch (Exception ex) {
            throw new ServiceUnavailableException("El servicio de contenido no está disponible", ex);
        }

        if (leccion == null) {
            throw ResourceNotFoundException.of("Lección", leccionId);
        }
        return leccion;
    }

    /** Consulta el título de una lección tolerando fallos del servicio de contenido. */
    protected LeccionFeign intentarLeccion(Long leccionId) {
        try {
            return contenidoClient.obtenerLeccion(leccionId);
        } catch (Exception ex) {
            log.warn("No fue posible consultar la lección {}: {}", leccionId, ex.getMessage());
            return null;
        }
    }

    protected String nombreUsuario(Long usuarioId) {
        try {
            UsuarioFeign usuario = authClient.obtenerUsuario(usuarioId);
            return (usuario == null || usuario.nombre() == null)
                    ? "Usuario #" + usuarioId
                    : usuario.nombre();
        } catch (Exception ex) {
            log.warn("No fue posible consultar el usuario {}: {}", usuarioId, ex.getMessage());
            return "Usuario #" + usuarioId;
        }
    }

    protected Map<Long, String> nombresUsuarios(Collection<Long> ids) {
        return ids.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::nombreUsuario, (a, b) -> a));
    }

    protected List<LeccionFeign> todasLasLecciones() {
        try {
            return contenidoClient.listarLecciones(5000).content();
        } catch (FeignException ex) {
            throw new ServiceUnavailableException("El servicio de contenido no está disponible", ex);
        } catch (Exception ex) {
            throw new ServiceUnavailableException("El servicio de contenido no está disponible", ex);
        }
    }

    protected static Set<Long> idsDe(Collection<Long> values) {
        return values.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }
}
