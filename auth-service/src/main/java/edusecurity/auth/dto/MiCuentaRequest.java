package edusecurity.auth.dto;

import edusecurity.auth.validation.PasswordSegura;
import jakarta.validation.constraints.Size;

/**
 * Actualización del perfil del usuario autenticado (/api/auth/me).
 * Para cambiar la contraseña se debe enviar {@code passwordActual}.
 */
public record MiCuentaRequest(
        @Size(min = 2, max = 150)
        String nombre,

        String passwordActual,

        @PasswordSegura
        String passwordNuevo) {
}
