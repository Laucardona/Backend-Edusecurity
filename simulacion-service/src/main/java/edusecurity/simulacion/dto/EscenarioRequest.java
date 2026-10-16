package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Nivel;
import edusecurity.simulacion.entity.TipoEscenario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Escenario completo con su bandeja. Se crea sin publicar. */
public record EscenarioRequest(
        @NotNull TipoEscenario tipo,
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 1000) String descripcion,
        @NotBlank @Size(max = 5000) String contexto,
        @NotNull Nivel nivel,
        @NotNull @Min(60) @Max(3600) Integer duracionSegundos,
        @NotEmpty @Size(max = 20) @Valid List<MensajeRequest> mensajes) {
}
