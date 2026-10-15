package edusecurity.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * Validación del JWT en el Gateway (misma clave y claims que auth-service).
 * No emite tokens: solo verifica firma, vigencia y tipo "access".
 */
public class JwtValidator {

    private final SecretKey key;

    public JwtValidator(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("app.jwt.secret no está configurado");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretBase64);
        } catch (Exception ex) {
            bytes = secretBase64.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * @return true si el token es un access token válido (firma y vigencia correctas).
     */
    public boolean esAccessTokenValido(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tipo = claims.get("tipo", String.class);
            Date expiracion = claims.getExpiration();

            return "access".equals(tipo)
                    && expiracion != null
                    && expiracion.after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
