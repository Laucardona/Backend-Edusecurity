package edusecurity.auth.service;

import edusecurity.auth.dto.AuthResponse;
import edusecurity.auth.dto.LoginRequest;
import edusecurity.auth.dto.MiCuentaRequest;
import edusecurity.auth.dto.RegisterRequest;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.InvalidTokenException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.JwtProperties;
import edusecurity.common.security.JwtSupport;
import edusecurity.common.security.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtSupport jwtSupport;
    private final JwtProperties jwtProperties;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 UserDetailsService userDetailsService,
                                 UsuarioRepository usuarioRepository,
                                 RolRepository rolRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtSupport jwtSupport,
                                 JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSupport = jwtSupport;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Registro público. Siempre crea ESTUDIANTE (no permite auto-asignarse ADMIN).
     */
    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        String correo = request.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictException("El correo ya está registrado: " + correo);
        }

        Rol estudiante = rolRepository.findByNombre("ESTUDIANTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol ESTUDIANTE no configurado"));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setActivo(true);
        usuario.setRol(estudiante);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario registrado: {} (id={})", guardado.getCorreo(), guardado.getId());
        return UsuarioResponse.from(guardado);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo().trim().toLowerCase(),
                        request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        return emitirTokens(userDetails.getUsername());
    }

    /**
     * Rota el refresh token por uno nuevo (rotación simple, sin persistencia).
     */
    @Transactional(readOnly = true)
    public AuthResponse refrescar(String refreshToken) {
        TokenInfo info = jwtSupport.parseRefreshToken(refreshToken);

        Usuario usuario = usuarioRepository.findByCorreo(info.correo())
                .orElseThrow(() -> new InvalidTokenException("Usuario del token no existe"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new InvalidTokenException("Usuario desactivado");
        }

        return emitirTokens(usuario.getCorreo());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerMiCuenta(TokenInfo info) {
        return UsuarioResponse.from(buscarPorCorreo(info.correo()));
    }

    @Transactional
    public UsuarioResponse actualizarMiCuenta(TokenInfo info, MiCuentaRequest request) {
        Usuario usuario = buscarPorCorreo(info.correo());

        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre().trim());
        }

        boolean quiereCambiarPassword = request.passwordNuevo() != null && !request.passwordNuevo().isBlank();
        if (quiereCambiarPassword) {
            if (request.passwordActual() == null || request.passwordActual().isBlank()) {
                throw new BadRequestException("Debe indicar la contraseña actual para cambiarla");
            }
            if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
                throw new BadRequestException("La contraseña actual no es correcta");
            }
            usuario.setPassword(passwordEncoder.encode(request.passwordNuevo()));
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    private AuthResponse emitirTokens(String correo) {
        Usuario usuario = buscarPorCorreo(correo);
        String rol = (usuario.getRol() == null) ? "" : usuario.getRol().getNombre();

        String accessToken = jwtSupport.generateAccessToken(usuario.getCorreo(), usuario.getId(), rol);
        String refreshToken = jwtSupport.generateRefreshToken(usuario.getCorreo(), usuario.getId(), rol);

        return AuthResponse.bearer(accessToken, refreshToken,
                jwtProperties.getAccessExpiration().toSeconds(),
                UsuarioResponse.from(usuario));
    }

    private Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + correo));
    }
}
