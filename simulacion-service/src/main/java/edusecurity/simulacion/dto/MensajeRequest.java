package edusecurity.simulacion.dto;

import edusecurity.simulacion.entity.Accion;
import edusecurity.simulacion.entity.Canal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MensajeRequest(
        @NotNull Canal canal,
        @NotBlank @Size(max = 150) String remitenteNombre,
        @NotBlank @Size(max = 200) String remitenteDireccion,
        @Size(max = 200) String asunto,
        @NotBlank @Size(max = 5000) String cuerpo,
        @Size(max = 300) String enlaceMostrado,
        @Size(max = 300) String enlaceReal,
        @Size(max = 150) String adjuntoNombre,
        @Size(max = 50) String recibidoEn,
        @NotNull Boolean esPhishing,
        @NotNull Accion accionCorrecta,
        @NotBlank @Size(max = 3000) String explicacion,
        @Valid List<IndicadorRequest> indicadores) {
}
