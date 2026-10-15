package edusecurity.progreso.client;

import edusecurity.common.web.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente del microservicio de autenticación (usuarios).
 */
@FeignClient(name = "edusecurity-auth", path = "/api/usuarios")
public interface AuthClient {

    @GetMapping("/{id}")
    UsuarioFeign obtenerUsuario(@PathVariable("id") Long id);

    @GetMapping
    PageResponse<UsuarioFeign> listarUsuarios(@RequestParam("size") int size);
}
