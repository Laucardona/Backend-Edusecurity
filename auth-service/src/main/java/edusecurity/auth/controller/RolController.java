package edusecurity.auth.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('roles:administrar') or hasRole('INTERNAL')")
@Tag(name = "Roles", description = "Administración de roles (solo ADMIN o servicios internos)")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @Operation(summary = "Listar roles")
    public List<RolResponse> listar() {
        return rolService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un rol por id")
    public RolResponse obtener(@PathVariable Long id) {
        return rolService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un rol")
    public RolResponse crear(@Valid @RequestBody RolRequest request) {
        return rolService.crear(request);
    }
}
