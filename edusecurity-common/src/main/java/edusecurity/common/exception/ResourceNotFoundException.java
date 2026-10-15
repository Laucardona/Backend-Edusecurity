package edusecurity.common.exception;

/**
 * Recurso inexistente (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String recurso, Object id) {
        return new ResourceNotFoundException(recurso + " no encontrado(a) con id " + id);
    }
}
