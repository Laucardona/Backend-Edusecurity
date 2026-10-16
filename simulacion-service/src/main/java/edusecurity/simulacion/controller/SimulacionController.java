package edusecurity.simulacion.controller;

import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import edusecurity.simulacion.dto.AccionRequest;
import edusecurity.simulacion.dto.AccionResponse;
import edusecurity.simulacion.dto.EscenarioDetalle;
import edusecurity.simulacion.dto.EscenarioRequest;
import edusecurity.simulacion.dto.EscenarioResumen;
import edusecurity.simulacion.dto.IndicadorCatalogo;
import edusecurity.simulacion.dto.PasoResponse;
import edusecurity.simulacion.dto.PublicacionRequest;
import edusecurity.simulacion.dto.RespuestaRequest;
import edusecurity.simulacion.dto.RespuestaResponse;
import edusecurity.simulacion.dto.ResultadoResponse;
import edusecurity.simulacion.dto.SesionResponse;
import edusecurity.simulacion.dto.SesionResumen;
import edusecurity.simulacion.entity.Nivel;
import edusecurity.simulacion.entity.TipoEscenario;
import edusecurity.simulacion.service.EscenarioService;
import edusecurity.simulacion.service.SesionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/simulaciones")
@Tag(name = "Simulaciones", description = "Laboratorios de phishing con bandeja simulada (HU43) y laboratorios "
        + "guiados por pasos (HU44), con tiempo límite y retroalimentación")
public class SimulacionController {

    private final EscenarioService escenarioService;
    private final SesionService sesionService;

    public SimulacionController(EscenarioService escenarioService, SesionService sesionService) {
        this.escenarioService = escenarioService;
        this.sesionService = sesionService;
    }

    // ------------------------------------------------------------------ catálogo

    @GetMapping
    @Operation(summary = "Listar escenarios", description = "El estudiante ve solo los publicados; ADMIN ve todos. Filtros: tipo, nivel.")
    public PageResponse<EscenarioResumen> listar(@RequestParam(required = false) TipoEscenario tipo,
                                                 @RequestParam(required = false) Nivel nivel,
                                                 @AuthenticationPrincipal TokenInfo principal,
                                                 @PageableDefault(size = 20, sort = "titulo") Pageable pageable) {
        return escenarioService.listar(tipo, nivel, principal, pageable);
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Catálogo de señales de alerta que el estudiante puede marcar")
    public List<IndicadorCatalogo> indicadores() {
        return escenarioService.catalogoIndicadores();
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Ver un escenario y su contexto (sin respuestas)")
    public EscenarioDetalle obtener(@PathVariable String uuid, @AuthenticationPrincipal TokenInfo principal) {
        return escenarioService.obtener(uuid, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('simulaciones:editar')")
    @Operation(summary = "Crear un escenario con su bandeja (simulaciones:editar)", description = "Se crea sin publicar.")
    public EscenarioDetalle crear(@Valid @RequestBody EscenarioRequest request) {
        return escenarioService.crear(request);
    }

    @PatchMapping("/{uuid}/publicacion")
    @PreAuthorize("hasAuthority('simulaciones:editar')")
    @Operation(summary = "Publicar o despublicar un escenario (simulaciones:editar)")
    public EscenarioDetalle publicar(@PathVariable String uuid, @Valid @RequestBody PublicacionRequest request) {
        return escenarioService.cambiarPublicacion(uuid, request);
    }

    // ------------------------------------------------------------------ laboratorio

    @PostMapping("/{uuid}/sesiones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar el laboratorio",
            description = "Devuelve la bandeja y el tiempo límite. Si ya hay una sesión en curso, la devuelve.")
    public SesionResponse iniciar(@PathVariable String uuid, @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.iniciar(uuid, principal);
    }

    @GetMapping("/sesiones/mias")
    @Operation(summary = "Mis sesiones (historial)")
    public PageResponse<SesionResumen> misSesiones(@AuthenticationPrincipal TokenInfo principal,
                                                   @PageableDefault(size = 20, sort = "iniciadaEn",
                                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return sesionService.misSesiones(principal, pageable);
    }

    @GetMapping("/sesiones/{sesionUuid}")
    @Operation(summary = "Estado de una sesión: bandeja, respuestas y segundos restantes")
    public SesionResponse obtenerSesion(@PathVariable String sesionUuid, @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.obtener(sesionUuid, principal);
    }

    @PutMapping("/sesiones/{sesionUuid}/respuestas/{mensajeId}")
    @Operation(summary = "Responder un mensaje (solo phishing)",
            description = "Clasificación (PHISHING o LEGITIMO), acción y señales encontradas. Se puede cambiar hasta finalizar. "
                    + "En un laboratorio guiado devuelve 400.")
    public RespuestaResponse responder(@PathVariable String sesionUuid, @PathVariable Long mensajeId,
                                       @Valid @RequestBody RespuestaRequest request,
                                       @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.responder(sesionUuid, mensajeId, request, principal);
    }

    @GetMapping("/sesiones/{sesionUuid}/paso")
    @Operation(summary = "Paso actual de un laboratorio guiado",
            description = "Situación, recurso simulado y opciones, sin puntos, respuesta correcta ni a dónde lleva cada opción.")
    public PasoResponse pasoActual(@PathVariable String sesionUuid, @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.pasoActual(sesionUuid, principal);
    }

    @PostMapping("/sesiones/{sesionUuid}/acciones")
    @Operation(summary = "Elegir una opción en el paso actual de un laboratorio guiado",
            description = "Cada paso se responde una vez. Devuelve la retroalimentación y el siguiente paso, "
                    + "o finalizada=true si el laboratorio terminó. 409 si el paso no es el actual; "
                    + "400 si la opción no pertenece al paso.")
    public AccionResponse registrarAccion(@PathVariable String sesionUuid, @Valid @RequestBody AccionRequest request,
                                          @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.registrarAccion(sesionUuid, request, principal);
    }

    @PostMapping("/sesiones/{sesionUuid}/finalizar")
    @Operation(summary = "Finalizar el laboratorio y ver la retroalimentación")
    public ResultadoResponse finalizar(@PathVariable String sesionUuid, @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.finalizar(sesionUuid, principal);
    }

    @GetMapping("/sesiones/{sesionUuid}/resultado")
    @Operation(summary = "Resultado de una sesión terminada")
    public ResultadoResponse resultado(@PathVariable String sesionUuid, @AuthenticationPrincipal TokenInfo principal) {
        return sesionService.resultado(sesionUuid, principal);
    }
}
