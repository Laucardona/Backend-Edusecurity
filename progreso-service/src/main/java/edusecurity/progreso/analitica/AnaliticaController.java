package edusecurity.progreso.analitica;

import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analitica")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Analítica", description = "Panel administrativo (HU33)")
public class AnaliticaController {

    private final PanelAdministrativoService panelService;

    public AnaliticaController(PanelAdministrativoService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/panel")
    @Operation(summary = "Panel administrativo (ADMIN)",
            description = "Usuarios, progreso y simulaciones del periodo [desde, hasta] (YYYY-MM-DD, UTC). "
                    + "Por defecto, los últimos 30 días; máximo 366. Si una fuente no responde, su bloque es null "
                    + "y aparece en fuentesNoDisponibles. certificados es null hasta implementar RF22.")
    public PanelAdministrativoResponse panel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return panelService.panel(desde, hasta);
    }
}
