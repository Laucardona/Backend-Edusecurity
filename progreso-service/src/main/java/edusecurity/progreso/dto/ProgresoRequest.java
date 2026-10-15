package edusecurity.progreso.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Registro o actualización de progreso (upsert por usuario + lección).
 * Si usuarioId es null se usa el usuario autenticado; solo ADMIN puede
 * registrar progreso a nombre de otro usuario.
 */
public record ProgresoRequest(

        Long usuarioId,

        @NotNull(message = "la lección es obligatoria")
        Long leccionId,

        @NotNull(message = "el porcentaje es obligatorio")
        @DecimalMin(value = "0", message = "el porcentaje mínimo es 0")
        @DecimalMax(value = "100", message = "el porcentaje máximo es 100")
        Double porcentaje,

        @NotNull(message = "el campo completado es obligatorio")
        Boolean completado) {
}
