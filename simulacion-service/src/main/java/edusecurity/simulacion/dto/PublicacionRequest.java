package edusecurity.simulacion.dto;

import jakarta.validation.constraints.NotNull;

public record PublicacionRequest(@NotNull Boolean publicado) {
}
