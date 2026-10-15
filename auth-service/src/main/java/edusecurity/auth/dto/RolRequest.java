package edusecurity.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RolRequest(
        @NotBlank(message = "el nombre del rol es obligatorio")
        @Size(max = 60)
        String nombre,

        @Size(max = 250)
        String descripcion) {
}
