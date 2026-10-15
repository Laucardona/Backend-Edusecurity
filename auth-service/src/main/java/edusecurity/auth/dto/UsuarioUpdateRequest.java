package edusecurity.auth.dto;

import edusecurity.auth.validation.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Actualización parcial: los campos en null se conservan.
 */
public record UsuarioUpdateRequest(
        @Size(min = 2, max = 150)
        String nombre,

        @Email(message = "el correo no es válido")
        String correo,

        @PasswordSegura
        String password,

        Long rolId) {
}
