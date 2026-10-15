package edusecurity.auth.dto;

/**
 * Cierre de sesión. Si se envía el refresh token se cierra esa sesión;
 * con {@code todas=true} se cierran todas las sesiones del usuario.
 */
public record LogoutRequest(String refreshToken, Boolean todas) {

    public boolean cerrarTodas() {
        return Boolean.TRUE.equals(todas);
    }
}
