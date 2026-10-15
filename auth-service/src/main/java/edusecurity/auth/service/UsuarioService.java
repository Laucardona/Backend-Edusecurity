package edusecurity.auth.service;

import edusecurity.auth.dto.EstadoRequest;
import edusecurity.auth.dto.UsuarioRequest;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.dto.UsuarioUpdateRequest;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String q, Pageable pageable) {
        String consulta = (q == null || q.isBlank()) ? null : q.trim();
        Page<Usuario> page = usuarioRepository.buscar(consulta, pageable);
        return PageResponse.from(page, UsuarioResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String correo = request.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictException("El correo ya está registrado: " + correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setActivo(true);
        usuario.setRol(resolverRol(request.rolId()));

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado por administración: {} (id={})", guardado.getCorreo(), guardado.getId());
        return UsuarioResponse.from(guardado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscar(id);

        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre().trim());
        }

        if (request.correo() != null && !request.correo().isBlank()) {
            String correo = request.correo().trim().toLowerCase();
            if (!correo.equals(usuario.getCorreo()) && usuarioRepository.existsByCorreo(correo)) {
                throw new ConflictException("El correo ya está registrado: " + correo);
            }
            usuario.setCorreo(correo);
        }

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.rolId() != null) {
            usuario.setRol(resolverRol(request.rolId()));
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, EstadoRequest request, TokenInfo actual) {
        Usuario usuario = buscar(id);

        if (Boolean.FALSE.equals(request.activo()) && actual.usuarioId() != null
                && actual.usuarioId().equals(id)) {
            throw new BadRequestException("No puede desactivar su propia cuenta");
        }

        usuario.setActivo(request.activo());
        if (Boolean.TRUE.equals(request.activo())) {
            // Activar también levanta un bloqueo temporal por intentos fallidos (HU38).
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, TokenInfo actual) {
        Usuario usuario = buscar(id);

        if (actual.usuarioId() != null && actual.usuarioId().equals(id)) {
            throw new BadRequestException("No puede eliminar su propia cuenta");
        }

        usuarioRepository.delete(usuario);
        log.info("Usuario eliminado: id={} ({})", id, usuario.getCorreo());
    }

    private Rol resolverRol(Long rolId) {
        Long id = (rolId == null) ? null : rolId;
        if (id == null) {
            return rolRepository.findByNombre("ESTUDIANTE")
                    .orElseThrow(() -> new ResourceNotFoundException("Rol por defecto no configurado"));
        }
        return rolRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }
}
