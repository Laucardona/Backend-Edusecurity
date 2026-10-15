package edusecurity.auth.dto;

import jakarta.validation.constraints.NotNull;

public record FortalezaPasswordRequest(
        @NotNull(message = "la contraseña es obligatoria")
        String password) {
}
