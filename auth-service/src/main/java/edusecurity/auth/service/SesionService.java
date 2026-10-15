package edusecurity.auth.service;

import edusecurity.auth.dto.SesionResponse;
import edusecurity.auth.entity.Sesion;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.SesionRepository;
import edusecurity.auth.security.TokenHasher;
import edusecurity.common.exception.InvalidTokenException;
import edusecurity.common.security.JwtProperties;
import edusecurity.common.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sesiones persistidas (HU3 cierre de sesión, HU37 historial). Cada refresh token
 * pertenece a una sesión; si la sesión se cierra, su refresh token deja de servir.
 */
@Service
public class SesionService {

    private final SesionRepository sesionRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public SesionService(SesionRepository sesionRepository, JwtProperties jwtProperties, Clock clock) {
        this.sesionRepository = sesionRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public void abrir(Usuario usuario, String refreshToken, ClienteInfo cliente) {
        Instant ahora = Instant.now(clock);
        Sesion sesion = new Sesion();
        sesion.setUsuario(usuario);
        sesion.setRefreshTokenHash(TokenHasher.sha256(refreshToken));
        sesion.setIp(cliente.ip());
        sesion.setDispositivo(cliente.dispositivo());
        sesion.setCreadaEn(ahora);
        sesion.setUltimoUso(ahora);
        sesion.setExpiraEn(ahora.plus(jwtProperties.getRefreshExpiration()));
        sesionRepository.save(sesion);
    }

    /**
     * Cambia el refresh token de la sesión por uno nuevo. Falla si la sesión no existe,
     * ya se cerró o venció (por ejemplo, si alguien reutiliza un refresh token viejo).
     */
    @Transactional
    public void rotar(String refreshTokenAnterior, String refreshTokenNuevo) {
        Instant ahora = Instant.now(clock);
        Sesion sesion = sesionRepository.findByRefreshTokenHash(TokenHasher.sha256(refreshTokenAnterior))
                .filter(s -> s.estaActiva(ahora))
                .orElseThrow(() -> new InvalidTokenException("La sesión fue cerrada o el token ya se usó"));

        sesion.setRefreshTokenHash(TokenHasher.sha256(refreshTokenNuevo));
        sesion.setUltimoUso(ahora);
        sesion.setExpiraEn(ahora.plus(jwtProperties.getRefreshExpiration()));
        sesionRepository.save(sesion);
    }

    /** Cierra la sesión del refresh token indicado si pertenece al usuario. Idempotente. */
    @Transactional
    public void cerrar(Long usuarioId, String refreshToken) {
        sesionRepository.findByRefreshTokenHash(TokenHasher.sha256(refreshToken))
                .filter(s -> s.getUsuario().getId().equals(usuarioId))
                .filter(s -> s.getRevocadaEn() == null)
                .ifPresent(s -> {
                    s.setRevocadaEn(Instant.now(clock));
                    sesionRepository.save(s);
                });
    }

    @Transactional
    public int cerrarTodas(Long usuarioId) {
        return sesionRepository.revocarTodas(usuarioId, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public PageResponse<SesionResponse> historial(Long usuarioId, Pageable pageable) {
        Instant ahora = Instant.now(clock);
        return PageResponse.from(sesionRepository.findByUsuarioIdOrderByCreadaEnDesc(usuarioId, pageable),
                sesion -> SesionResponse.from(sesion, ahora));
    }
}
