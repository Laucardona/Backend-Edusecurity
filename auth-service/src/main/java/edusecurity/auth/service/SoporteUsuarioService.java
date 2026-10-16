package edusecurity.auth.service;

import edusecurity.auth.dto.SesionResponse;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.web.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Acciones de soporte sobre una cuenta: desbloquear y revisar sesiones (tarea 1.3). */
@Service
public class SoporteUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(SoporteUsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final SesionService sesionService;

    public SoporteUsuarioService(UsuarioRepository usuarioRepository, SesionService sesionService) {
        this.usuarioRepository = usuarioRepository;
        this.sesionService = sesionService;
    }

    /** Levanta el bloqueo temporal por intentos fallidos (HU38). No cambia si la cuenta está activa. */
    @Transactional
    public UsuarioResponse desbloquear(Long id) {
        Usuario usuario = buscar(id);
        usuario.setBloqueadoHasta(null);
        usuario.setIntentosFallidos(0);
        log.info("Cuenta desbloqueada por soporte: usuario={}", id);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /** Historial de sesiones de un usuario. Nunca incluye el hash del refresh token. */
    @Transactional(readOnly = true)
    public PageResponse<SesionResponse> sesiones(Long id, Pageable pageable) {
        buscar(id);
        return sesionService.historial(id, pageable);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }
}
