package edusecurity.auth.controller;

import edusecurity.auth.dto.PermisosRequest;
import edusecurity.auth.dto.RolPermisosResponse;
import edusecurity.auth.dto.RolRequest;
import edusecurity.auth.dto.RolResponse;
import edusecurity.auth.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('roles:administrar') or hasRole('INTERNAL')")
@Tag(name = "Roles", description = "Roles y sus permisos (roles:administrar)")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @Operation(summary = "Listar roles con sus permisos")
    public List<RolPermisosResponse> listar() {
        return rolService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un rol con sus permisos")
    public RolPermisosResponse obtener(@PathVariable Long id) {
        return rolService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('roles:administrar')")
    @Operation(summary = "Crear un rol (sin permisos)")
    public RolResponse crear(@Valid @RequestBody RolRequest request) {
        return rolService.crear(request);
    }

    @PutMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('roles:administrar')")
    @Operation(summary = "Reemplazar los permisos de un rol",
            description = "400 si algún permiso no existe o si el rol ADMIN perdería roles:administrar. "
                    + "Los usuarios del rol reciben los permisos nuevos en su próximo access token.")
    public RolPermisosResponse cambiarPermisos(@PathVariable Long id, @Valid @RequestBody PermisosRequest request) {
        return rolService.cambiarPermisos(id, request);
    }
}
