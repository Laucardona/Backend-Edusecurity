package edusecurity.contenido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeccionRequest(
        @NotBlank(message = "el título es obligatorio")
        @Size(max = 150)
        String titulo,

        @Size(max = 1000)
        String descripcion,

        @Size(max = 5000)
        String contenido,

        Integer orden,

        Boolean activo,

        @NotNull(message = "el módulo es obligatorio")
        Long moduloId) {
}
