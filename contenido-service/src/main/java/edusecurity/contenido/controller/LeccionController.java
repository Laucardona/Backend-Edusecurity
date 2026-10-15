package edusecurity.contenido.controller;

import edusecurity.contenido.dto.LeccionRequest;
import edusecurity.contenido.dto.LeccionResponse;
import edusecurity.contenido.service.LeccionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecciones")
@Tag(name = "Lecciones", description = "Lecciones dentro de un módulo educativo")
public class LeccionController {

    private final LeccionService leccionService;

    public LeccionController(LeccionService leccionService) {
        this.leccionService = leccionService;
    }

    @GetMapping
    @Operation(summary = "Listar lecciones paginadas", description = "Filtros opcionales: q y moduloId.")
    public PageResponse<LeccionResponse> listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long moduloId,
            @PageableDefault(size = 20, sort = "orden") Pageable pageable) {
        return leccionService.listar(q, moduloId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una lección por id")
    public LeccionResponse obtener(@PathVariable Long id) {
        return leccionService.obtener(id);
    }

    @GetMapping("/modulo/{moduloId}")
    @Operation(summary = "Listar lecciones por módulo", description = "Ruta clásica compatible con el monolito original.")
    public PageResponse<LeccionResponse> porModulo(@PathVariable Long moduloId,
                                                   @PageableDefault(size = 50, sort = "orden") Pageable pageable) {
        return leccionService.listar("", moduloId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una lección (ADMIN)")
    public LeccionResponse crear(@Valid @RequestBody LeccionRequest request) {
        return leccionService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una lección (ADMIN)")
    public LeccionResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody LeccionRequest request) {
        return leccionService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una lección (ADMIN)")
    public void eliminar(@PathVariable Long id) {
        leccionService.eliminar(id);
    }
}
