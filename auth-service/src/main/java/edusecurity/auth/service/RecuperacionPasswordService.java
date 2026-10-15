package edusecurity.auth.service;

import edusecurity.auth.config.AccesoProperties;
import edusecurity.auth.entity.TokenRecuperacion;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.notification.CorreoService;
import edusecurity.auth.repository.TokenRecuperacionRepository;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.auth.security.TokenHasher;
import edusecurity.common.exception.BadRequestException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperación de contraseña por correo (HU4) con tokens de un solo uso.
 */
@Service
public class RecuperacionPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordService.class);

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final SesionService sesionService;
    private final CorreoService correoService;
    private final PasswordEncoder passwordEncoder;
    private final AccesoProperties properties;
    private final Clock clock;

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository,
                                       TokenRecuperacionRepository tokenRepository,
                                       SesionService sesionService,
                                       CorreoService correoService,
                                       PasswordEncoder passwordEncoder,
                                       AccesoProperties properties,
                                       Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.sesionService = sesionService;
        this.correoService = correoService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Genera un enlace de recuperación y lo envía al correo. Responde igual exista o no
     * la cuenta, para no revelar qué correos están registrados.
     */
    @Transactional
    public void solicitar(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase()).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            return;
        }

        Instant ahora = Instant.now(clock);
        tokenRepository.invalidarPendientes(usuario.getId(), ahora);

        String token = TokenHasher.generarTokenAleatorio();
        TokenRecuperacion registro = new TokenRecuperacion();
        registro.setUsuario(usuario);
        registro.setTokenHash(TokenHasher.sha256(token));
        registro.setCreadoEn(ahora);
        registro.setExpiraEn(ahora.plus(properties.getVigenciaRecuperacion()));
        tokenRepository.save(registro);

        String enlace = properties.getUrlRestablecer() + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        correoService.enviarRecuperacion(usuario.getCorreo(), usuario.getNombre(), enlace);
        log.info("Solicitud de recuperación de contraseña: usuario id={}", usuario.getId());
    }

    /**
     * Cambia la contraseña con un token válido, desbloquea la cuenta y cierra todas sus sesiones.
     */
    @Transactional
    public void restablecer(String token, String passwordNuevo) {
        Instant ahora = Instant.now(clock);
        TokenRecuperacion registro = tokenRepository.findByTokenHash(TokenHasher.sha256(token))
                .filter(t -> t.esValido(ahora))
                .orElseThrow(() -> new BadRequestException("El enlace de recuperación no es válido o ya venció"));

        Usuario usuario = registro.getUsuario();
        usuario.setPassword(passwordEncoder.encode(passwordNuevo));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        registro.setUsadoEn(ahora);
        tokenRepository.save(registro);

        sesionService.cerrarTodas(usuario.getId());
        log.info("Contraseña restablecida: usuario id={}", usuario.getId());
    }
}
