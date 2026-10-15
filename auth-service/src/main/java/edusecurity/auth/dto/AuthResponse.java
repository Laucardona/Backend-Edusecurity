package edusecurity.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario) {

    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresIn,
                                      UsuarioResponse usuario) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, usuario);
    }
}
