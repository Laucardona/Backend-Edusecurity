package edusecurity.contenido.controller;

import edusecurity.contenido.dto.EstadoRequest;
import edusecurity.contenido.dto.LeccionResponse;
import edusecurity.contenido.dto.ModuloRequest;
import edusecurity.contenido.dto.ModuloResponse;
import edusecurity.contenido.service.ModuloService;
import edusecurity.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/modulos")
@Tag(name = "Módulos", description = "Módulos educativos (lectura: cualquier usuario autenticado, escritura: ADMIN)")
public class ModuloController {

    private final ModuloService moduloService;

    public ModuloController(ModuloService moduloService) {
        this.moduloService = moduloService;
    }

    @GetMapping
    @Operation(summary = "Listar módulos paginados", description = "Filtros opcionales: q (búsqueda) y soloActivos.")
    public PageResponse<ModuloResponse> listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "false") boolean soloActivos,
            @PageableDefault(size = 20, sort = "orden") Pageable pageable) {
        return moduloService.listar(q, soloActivos, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un módulo con su total de lecciones")
    public ModuloResponse obtener(@PathVariable Long id) {
        return moduloService.obtener(id);
    }

    @GetMapping("/{id}/lecciones")
    @Operation(summary = "Listar las lecciones de un módulo (paginado)")
    public PageResponse<LeccionResponse> listarLecciones(@PathVariable Long id,
                                                         @PageableDefault(size = 50, sort = "orden") Pageable pageable) {
        return moduloService.listarLecciones(id, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('contenido:editar')")
    @Operation(summary = "Crear un módulo (contenido:editar)")
    public ModuloResponse crear(@Valid @RequestBody ModuloRequest request) {
        return moduloService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('contenido:editar')")
    @Operation(summary = "Actualizar un módulo (contenido:editar)")
    public ModuloResponse actualizar(@PathVariable Long id,
                                     @Valid @RequestBody ModuloRequest request) {
        return moduloService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('contenido:publicar')")
    @Operation(summary = "Activar o desactivar un módulo (contenido:publicar)")
    public ModuloResponse cambiarEstado(@PathVariable Long id,
                                        @Valid @RequestBody EstadoRequest request) {
        return moduloService.cambiarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('contenido:editar')")
    @Operation(summary = "Eliminar un módulo (contenido:editar)",
            description = "Si el módulo tiene lecciones se debe enviar force=true.")
    public void eliminar(@PathVariable Long id,
                         @RequestParam(defaultValue = "false") boolean force) {
        moduloService.eliminar(id, force);
    }
}
