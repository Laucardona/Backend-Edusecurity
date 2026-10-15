package edusecurity.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta de error estándar usada por todos los microservicios.
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    public ApiError {
        timestamp = (timestamp == null) ? Instant.now().toString() : timestamp;
        fieldErrors = (fieldErrors == null) ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().toString(), status, error, message, path, List.of());
    }
}
