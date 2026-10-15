package edusecurity.progreso.client;

import edusecurity.common.web.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente del microservicio de contenido educativo (módulos y lecciones).
 */
@FeignClient(name = "edusecurity-contenido", path = "/api/lecciones")
public interface ContenidoClient {

    @GetMapping("/{id}")
    LeccionFeign obtenerLeccion(@PathVariable("id") Long id);

    @GetMapping
    PageResponse<LeccionFeign> listarLecciones(@RequestParam("size") int size);
}
