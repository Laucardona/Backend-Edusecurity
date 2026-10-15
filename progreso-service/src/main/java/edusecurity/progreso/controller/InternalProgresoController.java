package edusecurity.progreso.controller;

import edusecurity.progreso.dto.SimulacionCompletadaRequest;
import edusecurity.progreso.dto.SimulacionCompletadaResponse;
import edusecurity.progreso.service.SimulacionCompletadaService;
import edusecurity.progreso.service.SimulacionCompletadaService.Registro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints servicio a servicio. Solo aceptan la clave interna (X-Service-Key, rol INTERNAL).
 * El gateway solo enruta /api/**, así que /internal/** no es accesible desde fuera.
 */
@RestController
@RequestMapping("/internal/progreso")
@PreAuthorize("hasRole('INTERNAL')")
@Tag(name = "Progreso interno", description = "Llamadas de otros microservicios (rol INTERNAL)")
public class InternalProgresoController {

    private final SimulacionCompletadaService simulacionService;

    public InternalProgresoController(SimulacionCompletadaService simulacionService) {
        this.simulacionService = simulacionService;
    }

    @PostMapping("/simulaciones")
    @Operation(summary = "Registrar el resultado de una simulación terminada (INTERNAL)",
            description = "Idempotente por sesionUuid: 201 al crear, 200 con el registro existente si ya estaba.")
    public ResponseEntity<SimulacionCompletadaResponse> registrarSimulacion(
            @Valid @RequestBody SimulacionCompletadaRequest request) {
        Registro registro = simulacionService.registrar(request);
        return ResponseEntity.status(registro.creado() ? HttpStatus.CREATED : HttpStatus.OK).body(registro.simulacion());
    }
}
