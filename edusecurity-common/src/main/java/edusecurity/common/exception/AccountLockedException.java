package edusecurity.common.exception;

/**
 * Cuenta bloqueada temporalmente por intentos fallidos de inicio de sesión (HTTP 423).
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
