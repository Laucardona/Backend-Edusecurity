package edusecurity.contenido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModuloRequest(
        @NotBlank(message = "el título es obligatorio")
        @Size(max = 150, message = "el título no puede superar 150 caracteres")
        String titulo,

        @Size(max = 1000)
        String descripcion,

        Integer orden,

        Boolean activo) {
}
