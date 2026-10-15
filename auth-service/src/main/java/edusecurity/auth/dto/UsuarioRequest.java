package edusecurity.auth.dto;

import edusecurity.auth.validation.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creación de usuarios desde el panel de administración (permite elegir rol).
 */
public record UsuarioRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(min = 2, max = 150)
        String nombre,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no es válido")
        String correo,

        @NotBlank(message = "la contraseña es obligatoria")
        @PasswordSegura
        String password,

        Long rolId) {
}
