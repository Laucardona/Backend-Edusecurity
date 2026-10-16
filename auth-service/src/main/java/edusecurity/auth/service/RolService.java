package edusecurity.auth.service;

import edusecurity.auth.dto.PermisosRequest;
import edusecurity.auth.dto.RolPermisosResponse;
import edusecurity.auth.dto.RolRequest;
import edusecurity.auth.dto.RolResponse;
import edusecurity.auth.entity.Permiso;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.repository.PermisoRepository;
import edusecurity.auth.repository.RolRepository;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.Permisos;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolService {

    private static final Logger log = LoggerFactory.getLogger(RolService.class);

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    @Transactional(readOnly = true)
    public List<RolPermisosResponse> listar() {
        return rolRepository.findAllByOrderByNombreAsc().stream().map(RolPermisosResponse::from).toList();
    }

    @Transactional
    public RolResponse crear(RolRequest request) {
        String nombre = request.nombre().trim().toUpperCase();

        if (rolRepository.existsByNombre(nombre)) {
            throw new ConflictException("El rol ya existe: " + nombre);
        }

        Rol rol = new Rol(nombre, request.descripcion());
        return RolResponse.from(rolRepository.save(rol));
    }

    @Transactional(readOnly = true)
    public RolPermisosResponse obtener(Long id) {
        return rolRepository.findWithPermisosById(id)
                .map(RolPermisosResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
    }

    /**
     * Reemplaza los permisos del rol. Todos los códigos deben existir, y el rol ADMIN nunca pierde
     * roles:administrar (si no, nadie podría volver a asignar permisos). Los usuarios del rol reciben
     * los permisos nuevos en su próximo access token.
     */
    @Transactional
    public RolPermisosResponse cambiarPermisos(Long id, PermisosRequest request) {
        Rol rol = rolRepository.findWithPermisosById(id).orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
        Set<String> codigos = request.permisos().stream().map(String::trim).collect(Collectors.toCollection(TreeSet::new));

        if (Permisos.ADMIN.equals(rol.getNombre()) && !codigos.contains(Permisos.ROLES_ADMINISTRAR)) {
            throw new BadRequestException("El rol ADMIN no puede perder el permiso " + Permisos.ROLES_ADMINISTRAR);
        }

        List<Permiso> permisos = permisoRepository.findByCodigoIn(codigos);
        if (permisos.size() != codigos.size()) {
            Set<String> desconocidos = new TreeSet<>(codigos);
            permisos.forEach(p -> desconocidos.remove(p.getCodigo()));
            throw new BadRequestException("Permisos desconocidos: " + String.join(", ", desconocidos));
        }

        rol.setPermisos(new HashSet<>(permisos));
        Rol guardado = rolRepository.save(rol);
        log.info("Permisos del rol {} actualizados: {}", guardado.getNombre(), codigos);
        return RolPermisosResponse.from(guardado);
    }
}
