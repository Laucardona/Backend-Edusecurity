package edusecurity.common.error;

/**
 * Detalle de una validación de campo fallida.
 */
public record FieldErrorDetail(String field, String message) {
}
