package edusecurity.common.exception;

/**
 * El usuario autenticado no tiene permiso para la operación (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
