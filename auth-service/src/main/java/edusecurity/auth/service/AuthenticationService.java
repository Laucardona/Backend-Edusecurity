package edusecurity.auth.service;

import edusecurity.auth.dto.AuthResponse;
import edusecurity.auth.dto.FortalezaPasswordResponse;
import edusecurity.auth.dto.LoginRequest;
import edusecurity.auth.dto.LogoutRequest;
import edusecurity.auth.dto.MiCuentaRequest;
import edusecurity.auth.dto.RegisterRequest;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.auth.validation.PoliticaPassword;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.InvalidTokenException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.JwtProperties;
import edusecurity.common.security.JwtSupport;
import edusecurity.common.security.TokenInfo;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final IntentosLoginService intentosLoginService;
    private final SesionService sesionService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 UserDetailsService userDetailsService,
                                 UsuarioRepository usuarioRepository,
                                 RolRepository rolRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtSupport jwtSupport,
                                 JwtProperties jwtProperties,
                                 IntentosLoginService intentosLoginService,
                                 SesionService sesionService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSupport = jwtSupport;
        this.jwtProperties = jwtProperties;
        this.intentosLoginService = intentosLoginService;
        this.sesionService = sesionService;
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

    /**
     * Inicio de sesión con bloqueo temporal tras varios intentos fallidos (HU38).
     * Cada login abre una sesión nueva que queda en el historial (HU37).
     */
    @Transactional
    public AuthResponse login(LoginRequest request, ClienteInfo cliente) {
        String correo = request.correo().trim().toLowerCase();
        intentosLoginService.verificarNoBloqueado(correo);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, request.password()));
        } catch (BadCredentialsException ex) {
            Instant bloqueadoHasta = intentosLoginService.registrarFallo(correo);
            if (bloqueadoHasta != null) {
                throw intentosLoginService.bloqueada(bloqueadoHasta);
            }
            throw ex;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        Usuario usuario = buscarPorCorreo(userDetails.getUsername());
        intentosLoginService.registrarExito(usuario);

        AuthResponse respuesta = emitirTokens(usuario);
        sesionService.abrir(usuario, respuesta.refreshToken(), cliente);
        return respuesta;
    }

    /**
     * Rota el refresh token por uno nuevo. El anterior deja de servir, y si la sesión
     * fue cerrada (logout) el refresh token se rechaza.
     */
    @Transactional
    public AuthResponse refrescar(String refreshToken) {
        TokenInfo info = jwtSupport.parseRefreshToken(refreshToken);

        Usuario usuario = usuarioRepository.findByCorreo(info.correo())
                .orElseThrow(() -> new InvalidTokenException("Usuario del token no existe"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new InvalidTokenException("Usuario desactivado");
        }

        AuthResponse respuesta = emitirTokens(usuario);
        sesionService.rotar(refreshToken, respuesta.refreshToken());
        return respuesta;
    }

    /**
     * Cierre de sesión (HU3): invalida el refresh token de esta sesión, o el de todas.
     * El access token deja de servir cuando vence (vigencia corta).
     */
    @Transactional
    public void logout(TokenInfo principal, LogoutRequest request) {
        Long usuarioId = buscarPorCorreo(principal.correo()).getId();

        if (request != null && request.cerrarTodas()) {
            sesionService.cerrarTodas(usuarioId);
            return;
        }
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BadRequestException("Envíe el refreshToken de la sesión o todas=true");
        }
        sesionService.cerrar(usuarioId, request.refreshToken());
    }

    public FortalezaPasswordResponse evaluarPassword(String password) {
        int puntaje = PoliticaPassword.puntaje(password);
        var faltantes = PoliticaPassword.requisitosFaltantes(password);
        return new FortalezaPasswordResponse(puntaje, PoliticaPassword.nivel(puntaje),
                faltantes.isEmpty(), faltantes);
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
            sesionService.cerrarTodas(usuario.getId());
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    private AuthResponse emitirTokens(Usuario usuario) {
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
