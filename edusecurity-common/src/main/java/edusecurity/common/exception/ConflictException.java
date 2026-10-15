package edusecurity.common.exception;

/**
 * Conflicto con el estado actual del recurso, p.ej. registro duplicado (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
