package edusecurity.auth.controller;

import edusecurity.auth.dto.EstadoRequest;
import edusecurity.auth.dto.UsuarioRequest;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.dto.UsuarioUpdateRequest;
import edusecurity.auth.dto.SesionResponse;
import edusecurity.auth.service.SoporteUsuarioService;
import edusecurity.auth.service.UsuarioService;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasAuthority('usuarios:administrar') or hasRole('INTERNAL')")
@Tag(name = "Usuarios", description = "Administración de usuarios (usuarios:administrar) y soporte")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final SoporteUsuarioService soporteService;

    public UsuarioController(UsuarioService usuarioService, SoporteUsuarioService soporteService) {
        this.usuarioService = usuarioService;
        this.soporteService = soporteService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios paginados", description = "Búsqueda opcional por nombre o correo (parámetro q).")
    public PageResponse<UsuarioResponse> listar(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return usuarioService.listar(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por id")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un usuario (permite asignar rol)")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario (parcial)")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o desactivar un usuario")
    public UsuarioResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody EstadoRequest request,
                                         @AuthenticationPrincipal TokenInfo principal) {
        return usuarioService.cambiarEstado(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un usuario")
    public void eliminar(@PathVariable Long id,
                         @AuthenticationPrincipal TokenInfo principal) {
        usuarioService.eliminar(id, principal);
    }

    @PostMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('usuarios:desbloquear')")
    @Operation(summary = "Desbloquear una cuenta (usuarios:desbloquear)",
            description = "Levanta el bloqueo temporal por intentos fallidos y reinicia el contador.")
    public UsuarioResponse desbloquear(@PathVariable Long id) {
        return soporteService.desbloquear(id);
    }

    @GetMapping("/{id}/sesiones")
    @PreAuthorize("hasAuthority('usuarios:ver-sesiones')")
    @Operation(summary = "Historial de sesiones de un usuario (usuarios:ver-sesiones)",
            description = "Fecha, IP, dispositivo y si sigue activa. Nunca incluye el refresh token ni su hash.")
    public PageResponse<SesionResponse> sesiones(@PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return soporteService.sesiones(id, pageable);
    }
}
