package edusecurity.auth.controller;

import edusecurity.auth.dto.MetricasUsuariosResponse;
import edusecurity.auth.service.MetricasUsuariosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints servicio a servicio. Solo aceptan la clave interna (X-Service-Key, rol INTERNAL).
 * El gateway solo enruta /api/**, así que /internal/** no es accesible desde fuera.
 */
@RestController
@RequestMapping("/internal/usuarios")
@PreAuthorize("hasRole('INTERNAL')")
@Tag(name = "Usuarios internos", description = "Llamadas de otros microservicios (rol INTERNAL)")
public class InternalUsuarioController {

    private final MetricasUsuariosService metricasService;

    public InternalUsuarioController(MetricasUsuariosService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/metricas")
    @Operation(summary = "Métricas de usuarios y sesiones para el panel administrativo (INTERNAL)",
            description = "desde y hasta en formato YYYY-MM-DD, ambos incluidos (UTC).")
    public MetricasUsuariosResponse metricas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return metricasService.metricas(desde, hasta);
    }
}
