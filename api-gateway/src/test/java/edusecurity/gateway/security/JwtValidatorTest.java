package edusecurity.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtValidatorTest {

    private static final String SECRETO = "c29sby1wYXJhLXBydWViYXMtbm8tdXNhci1lbi1wcm9kISE=";

    private JwtValidator validator;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        validator = new JwtValidator(SECRETO);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRETO));
    }

    @Test
    void aceptaAccessTokenFirmadoCorrectamente() {
        String token = firmar("access", new Date(System.currentTimeMillis() + 60_000));

        assertThat(validator.esAccessTokenValido(token)).isTrue();
    }

    @Test
    void rechazaTokenExpirado() {
        String token = firmar("access", new Date(System.currentTimeMillis() - 60_000));

        assertThat(validator.esAccessTokenValido(token)).isFalse();
    }

    @Test
    void rechazaRefreshToken() {
        String token = firmar("refresh", new Date(System.currentTimeMillis() + 60_000));

        assertThat(validator.esAccessTokenValido(token)).isFalse();
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        SecretKey otra = Keys.hmacShaKeyFor(
                "otra-clave-completamente-diferente-para-el-test".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("laura@correo.com")
                .claim("tipo", "access")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otra)
                .compact();

        assertThat(validator.esAccessTokenValido(token)).isFalse();
    }

    @Test
    void rechazaTokenBasura() {
        assertThat(validator.esAccessTokenValido("no-es-un-token")).isFalse();
    }

    private String firmar(String tipo, Date expiracion) {
        return Jwts.builder()
                .subject("laura@correo.com")
                .claim("uid", 1L)
                .claim("rol", "ADMIN")
                .claim("tipo", tipo)
                .issuedAt(new Date())
                .expiration(expiracion)
                .signWith(key)
                .compact();
    }
}
