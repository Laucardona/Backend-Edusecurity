package edusecurity.progreso.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Resultado de una sesión de simulación cerrada, enviado por simulacion-service. */
public record SimulacionCompletadaRequest(
        @NotNull @Positive Long usuarioId,
        @NotBlank @Size(max = 36) String sesionUuid,
        @NotBlank @Size(max = 36) String escenarioUuid,
        @NotBlank @Size(max = 30) String tipo,
        @NotBlank @Size(max = 150) String titulo,
        @NotNull @PositiveOrZero Integer puntaje,
        @NotNull @PositiveOrZero Integer puntajeMaximo,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double porcentaje,
        @NotNull Boolean aprobado,
        @NotBlank @Pattern(regexp = "FINALIZADA|VENCIDA", message = "debe ser FINALIZADA o VENCIDA") String estado,
        @NotNull LocalDateTime finalizadaEn) {
}
