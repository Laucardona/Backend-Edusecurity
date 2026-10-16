package edusecurity.common.security;

import edusecurity.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * Emisión y validación de tokens JWT (HMAC SHA-256) compartida por los microservicios.
 *
 * <p>Claims personalizados: {@code uid} (id de usuario), {@code rol}, {@code tipo},
 * {@code permisos} (lista, solo en access) y {@code mfa} (booleano, solo en access).</p>
 */
public class JwtSupport {

    public static final String TIPO_ACCESO = "access";
    public static final String TIPO_REFRESCO = "refresh";

    private final JwtProperties properties;

    public JwtSupport(JwtProperties properties) {
        this.properties = properties;
    }

    /** Access token con los permisos del rol del usuario y el estado de su verificación en dos pasos. */
    public String generateAccessToken(String correo, Long usuarioId, String rol, Collection<String> permisos,
                                      boolean mfa) {
        return builder(correo, usuarioId, rol, TIPO_ACCESO, properties.getAccessExpiration())
                .claim("permisos", List.copyOf(permisos))
                .claim("mfa", mfa)
                .compact();
    }

    /**
     * Atajo para pruebas y herramientas: usa los permisos predeterminados del rol
     * ({@link Permisos#PREDETERMINADOS}) y {@code mfa=false}. auth-service emite los tokens reales
     * con {@link #generateAccessToken(String, Long, String, Collection, boolean)}.
     */
    public String generateAccessToken(String correo, Long usuarioId, String rol) {
        return generateAccessToken(correo, usuarioId, rol, Permisos.predeterminadosDe(rol), false);
    }

    public String generateRefreshToken(String correo, Long usuarioId, String rol) {
        return builder(correo, usuarioId, rol, TIPO_REFRESCO, properties.getRefreshExpiration()).compact();
    }

    private JwtBuilder builder(String correo, Long usuarioId, String rol, String tipo, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(correo)
                .claim("uid", usuarioId)
                .claim("rol", rol)
                .claim("tipo", tipo)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey());
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
        List<?> permisos = claims.get("permisos", List.class);
        Boolean mfa = claims.get("mfa", Boolean.class);
        return new TokenInfo(
                claims.getSubject(),
                (uid == null) ? null : uid.longValue(),
                claims.get("rol", String.class),
                tipo,
                (permisos == null) ? List.of() : permisos.stream().map(String::valueOf).toList(),
                Boolean.TRUE.equals(mfa));
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
