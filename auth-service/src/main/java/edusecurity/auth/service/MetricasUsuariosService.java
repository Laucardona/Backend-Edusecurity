package edusecurity.auth.service;

import edusecurity.common.exception.BadRequestException;
import edusecurity.auth.dto.MetricasUsuariosResponse;
import edusecurity.auth.repository.SesionRepository;
import edusecurity.auth.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Métricas agregadas de usuarios y sesiones para el panel administrativo (HU33). Solo consultas COUNT. */
@Service
public class MetricasUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final Clock clock;

    public MetricasUsuariosService(UsuarioRepository usuarioRepository, SesionRepository sesionRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.clock = clock;
    }

    /** El periodo incluye ambos días completos, en UTC. */
    @Transactional(readOnly = true)
    public MetricasUsuariosResponse metricas(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        Instant inicio = desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant fin = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Long> porRol = new TreeMap<>();
        for (Object[] fila : usuarioRepository.contarPorRol()) {
            porRol.put(fila[0] == null ? "SIN_ROL" : (String) fila[0], (Long) fila[1]);
        }

        return new MetricasUsuariosResponse(desde, hasta,
                usuarioRepository.count(),
                usuarioRepository.countByActivoTrue(),
                porRol,
                usuarioRepository.countByBloqueadoHastaAfter(Instant.now(clock)),
                sesionRepository.contarUsuariosConSesionEntre(inicio, fin),
                sesionRepository.contarCreadasEntre(inicio, fin));
    }
}
