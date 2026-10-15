package edusecurity.auth.dto;

import edusecurity.auth.validation.PasswordSegura;
import jakarta.validation.constraints.NotBlank;

public record RestablecerPasswordRequest(
        @NotBlank(message = "el token es obligatorio")
        String token,

        @NotBlank(message = "la contraseña nueva es obligatoria")
        @PasswordSegura
        String passwordNuevo) {
}
