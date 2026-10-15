package edusecurity.common.exception;

/**
 * Solicitud inválida según reglas de negocio (HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
