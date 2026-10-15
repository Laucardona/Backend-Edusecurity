package edusecurity.auth.service;

import edusecurity.auth.config.AccesoProperties;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.common.exception.AccountLockedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloqueo temporal por intentos fallidos (HU38). Los fallos se guardan en una
 * transacción propia para que no se pierdan cuando el login lanza la excepción.
 */
@Service
public class IntentosLoginService {

    private static final Logger log = LoggerFactory.getLogger(IntentosLoginService.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("America/Bogota"));

    private final UsuarioRepository usuarioRepository;
    private final AccesoProperties properties;
    private final Clock clock;

    public IntentosLoginService(UsuarioRepository usuarioRepository, AccesoProperties properties,
                                Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void verificarNoBloqueado(String correo) {
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            if (usuario.estaBloqueado(Instant.now(clock))) {
                throw bloqueada(usuario.getBloqueadoHasta());
            }
        });
    }

    /**
     * Suma un intento fallido. Si con este intento la cuenta quedó bloqueada devuelve
     * hasta cuándo; si no, null.
     * Si el correo no existe no hace nada (no revela qué correos están registrados).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Instant registrarFallo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null) {
            return null;
        }

        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= properties.getMaxIntentosFallidos()) {
            usuario.setIntentosFallidos(0);
            Instant hasta = Instant.now(clock).plus(properties.getDuracionBloqueo());
            usuario.setBloqueadoHasta(hasta);
            usuarioRepository.save(usuario);
            log.warn("Cuenta bloqueada por {} intentos fallidos: usuario id={}",
                    properties.getMaxIntentosFallidos(), usuario.getId());
            return hasta;
        }

        usuario.setIntentosFallidos(intentos);
        usuarioRepository.save(usuario);
        return null;
    }

    @Transactional
    public void registrarExito(Usuario usuario) {
        if (usuario.getIntentosFallidos() != 0 || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }
    }

    public AccountLockedException bloqueada(Instant hasta) {
        return new AccountLockedException("La cuenta está bloqueada temporalmente por intentos fallidos. "
                + "Intente de nuevo después de las " + HORA.format(hasta)
                + " o recupere su contraseña.");
    }
}
