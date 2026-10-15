package edusecurity.progreso.client;

import edusecurity.common.web.PageResponse;
import java.time.LocalDate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente del microservicio de autenticación (usuarios y métricas internas).
 */
@FeignClient(name = "edusecurity-auth")
public interface AuthClient {

    @GetMapping("/api/usuarios/{id}")
    UsuarioFeign obtenerUsuario(@PathVariable("id") Long id);

    @GetMapping("/api/usuarios")
    PageResponse<UsuarioFeign> listarUsuarios(@RequestParam("size") int size);

    /** Métricas para el panel administrativo (HU33). Ruta interna: requiere X-Service-Key. */
    @GetMapping("/internal/usuarios/metricas")
    MetricasUsuariosFeign obtenerMetricas(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta);
}
