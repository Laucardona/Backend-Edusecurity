package edusecurity.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "el token de refresco es obligatorio")
        String refreshToken) {
}
