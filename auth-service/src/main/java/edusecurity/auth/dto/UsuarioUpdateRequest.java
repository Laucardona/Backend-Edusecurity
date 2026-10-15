package edusecurity.auth.dto;

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

        @Size(min = 6, max = 72)
        String password,

        Long rolId) {
}
