package edusecurity.common.exception;

/**
 * Un servicio del que depende la petición no está disponible o rechazó la
 * llamada interna (HTTP 503).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
