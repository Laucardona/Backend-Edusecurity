package edusecurity.common.security;

import edusecurity.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * Emisión y validación de tokens JWT (HMAC SHA-256) compartida por los microservicios.
 *
 * <p>Claims personalizados: {@code uid} (id de usuario), {@code rol} y {@code tipo}.</p>
 */
public class JwtSupport {

    public static final String TIPO_ACCESO = "access";
    public static final String TIPO_REFRESCO = "refresh";

    private final JwtProperties properties;

    public JwtSupport(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateAccessToken(String correo, Long usuarioId, String rol) {
        return generate(correo, usuarioId, rol, TIPO_ACCESO, properties.getAccessExpiration());
    }

    public String generateRefreshToken(String correo, Long usuarioId, String rol) {
        return generate(correo, usuarioId, rol, TIPO_REFRESCO, properties.getRefreshExpiration());
    }

    private String generate(String correo, Long usuarioId, String rol, String tipo, java.time.Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(correo)
                .claim("uid", usuarioId)
                .claim("rol", rol)
                .claim("tipo", tipo)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey())
                .compact();
    }

    /** Valida firma y vigencia de un token y exige el tipo de token indicado. */
    public TokenInfo parse(String token, String tipoEsperado) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token inválido o expirado", ex);
        }

        String tipo = claims.get("tipo", String.class);
        if (tipoEsperado != null && !tipoEsperado.equals(tipo)) {
            throw new InvalidTokenException("Tipo de token incorrecto: se esperaba '" + tipoEsperado + "'");
        }

        Number uid = claims.get("uid", Number.class);
        return new TokenInfo(
                claims.getSubject(),
                (uid == null) ? null : uid.longValue(),
                claims.get("rol", String.class),
                tipo);
    }

    public TokenInfo parseAccessToken(String token) {
        return parse(token, TIPO_ACCESO);
    }

    public TokenInfo parseRefreshToken(String token) {
        return parse(token, TIPO_REFRESCO);
    }

    private SecretKey signingKey() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret no está configurado");
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
