package edusecurity.auth.dto;

import edusecurity.auth.validation.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(min = 2, max = 150, message = "el nombre debe tener entre 2 y 150 caracteres")
        String nombre,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no es válido")
        @Size(max = 150, message = "el correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "la contraseña es obligatoria")
        @PasswordSegura
        String password) {
}
