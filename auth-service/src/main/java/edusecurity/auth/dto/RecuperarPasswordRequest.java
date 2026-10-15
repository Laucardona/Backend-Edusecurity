package edusecurity.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarPasswordRequest(
        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no es válido")
        String correo) {
}
