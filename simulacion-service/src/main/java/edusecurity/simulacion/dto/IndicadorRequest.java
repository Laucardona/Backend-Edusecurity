package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.TipoIndicador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IndicadorRequest(
        @NotNull TipoIndicador tipo,
        @NotBlank @Size(max = 500) String descripcion,
        @Size(max = 300) String fragmento) {
}
