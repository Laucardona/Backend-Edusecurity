package edusecurity.progreso.controller;

import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import edusecurity.progreso.dto.ProgresoRequest;
import edusecurity.progreso.dto.ProgresoResponse;
import edusecurity.progreso.dto.ResumenGlobalResponse;
import edusecurity.progreso.dto.ResumenUsuarioResponse;
import edusecurity.progreso.dto.SimulacionCompletadaResponse;
import edusecurity.progreso.service.ProgresoService;
import edusecurity.progreso.service.ResumenService;
import edusecurity.progreso.service.SimulacionCompletadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progreso")
@Tag(name = "Progreso", description = "Seguimiento del avance de los estudiantes")
public class ProgresoController {

    private final ProgresoService progresoService;
    private final ResumenService resumenService;
    private final SimulacionCompletadaService simulacionService;

    public ProgresoController(ProgresoService progresoService, ResumenService resumenService,
                              SimulacionCompletadaService simulacionService) {
        this.progresoService = progresoService;
        this.resumenService = resumenService;
        this.simulacionService = simulacionService;
    }

    @PostMapping
    @Operation(summary = "Registrar o actualizar progreso (upsert)",
            description = "Si no se envía usuarioId se registra al usuario autenticado; "
                    + "solo ADMIN puede registrar a nombre de otro usuario.")
    public ProgresoResponse guardar(@Valid @RequestBody ProgresoRequest request,
                                    @AuthenticationPrincipal TokenInfo principal) {
        return progresoService.guardar(request, principal);
    }

    @GetMapping("/mio")
    @Operation(summary = "Listar mi progreso (paginado)")
    public PageResponse<ProgresoResponse> miProgreso(@AuthenticationPrincipal TokenInfo principal,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return progresoService.listarPorUsuario(principal.usuarioId(), pageable);
    }

    @GetMapping("/simulaciones/mias")
    @Operation(summary = "Mis simulaciones terminadas (phishing y laboratorios guiados), las más recientes primero")
    public PageResponse<SimulacionCompletadaResponse> misSimulaciones(
            @AuthenticationPrincipal TokenInfo principal,
            @PageableDefault(size = 20, sort = "finalizadaEn", direction = Sort.Direction.DESC) Pageable pageable) {
        return simulacionService.listarPorUsuario(principal.usuarioId(), pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "Listar todo el progreso (ADMIN)",
            description = "Filtros opcionales: usuarioId, leccionId y moduloId.")
    public PageResponse<ProgresoResponse> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long leccionId,
            @RequestParam(required = false) Long moduloId,
            @PageableDefault(size = 20) Pageable pageable) {
        return progresoService.listar(usuarioId, leccionId, moduloId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un registro de progreso",
            description = "El dueño solo puede ver el suyo; ADMIN/INTERNAL ven cualquiera.")
    public ProgresoResponse obtener(@PathVariable Long id,
                                    @AuthenticationPrincipal TokenInfo principal) {
        return progresoService.obtener(id, principal);
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "Listar el progreso de un usuario (ADMIN)")
    public PageResponse<ProgresoResponse> porUsuario(@PathVariable Long usuarioId,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return progresoService.listarPorUsuario(usuarioId, pageable);
    }

    @GetMapping("/leccion/{leccionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "Listar el progreso de una lección (ADMIN)")
    public PageResponse<ProgresoResponse> porLeccion(@PathVariable Long leccionId,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return progresoService.listarPorLeccion(leccionId, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un registro de progreso (ADMIN)")
    public void eliminar(@PathVariable Long id) {
        progresoService.eliminar(id);
    }

    @GetMapping("/resumen/mio")
    @Operation(summary = "Resumen de mi avance por módulo")
    public ResumenUsuarioResponse miResumen(@AuthenticationPrincipal TokenInfo principal) {
        return resumenService.resumenUsuario(principal.usuarioId());
    }

    @GetMapping("/resumen/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "Resumen de avance de un usuario (ADMIN)")
    public ResumenUsuarioResponse resumenUsuario(@PathVariable Long usuarioId) {
        return resumenService.resumenUsuario(usuarioId);
    }

    @GetMapping("/resumen/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "Estadísticas globales de la plataforma (ADMIN)")
    public ResumenGlobalResponse resumenGlobal() {
        return resumenService.resumenGlobal();
    }
}
