package edusecurity.auth.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "el estado 'activo' es obligatorio")
        Boolean activo) {
}
