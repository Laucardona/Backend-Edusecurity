package edusecurity.common.exception;

/**
 * Token ausente, malformado, con tipo incorrecto o expirado (HTTP 401).
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
