package edusecurity.simulacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Decisión del estudiante en el paso actual del laboratorio guiado. */
public record AccionRequest(
        @NotBlank @Size(max = 40) String pasoCodigo,
        @NotBlank @Size(max = 10) String opcionCodigo) {
}
