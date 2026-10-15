package edusecurity.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edusecurity.common.exception.InvalidTokenException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtSupportTest {

    /** Clave solo para pruebas; en ejecución real llega por la variable JWT_SECRET. */
    private static final String SECRETO_PRUEBA = "c29sby1wYXJhLXBydWViYXMtbm8tdXNhci1lbi1wcm9kISE=";

    private JwtSupport jwtSupport;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRETO_PRUEBA);
        properties.setAccessExpiration(Duration.ofHours(1));
        properties.setRefreshExpiration(Duration.ofDays(1));
        jwtSupport = new JwtSupport(properties);
    }

    @Test
    void generaYValidaTokenDeAcceso() {
        String token = jwtSupport.generateAccessToken("laura@correo.com", 7L, "ADMIN");

        TokenInfo info = jwtSupport.parseAccessToken(token);

        assertThat(info.correo()).isEqualTo("laura@correo.com");
        assertThat(info.usuarioId()).isEqualTo(7L);
        assertThat(info.rol()).isEqualTo("ADMIN");
        assertThat(info.tipo()).isEqualTo("access");
    }

    @Test
    void rechazaTokenDeRefrescoComoTokenDeAcceso() {
        String refresh = jwtSupport.generateRefreshToken("laura@correo.com", 7L, "ADMIN");

        assertThatThrownBy(() -> jwtSupport.parseAccessToken(refresh))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Tipo de token incorrecto");
    }

    @Test
    void rechazaTokenManipulado() {
        String token = jwtSupport.generateAccessToken("laura@correo.com", 7L, "ADMIN");
        String manipulado = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwtSupport.parseAccessToken(manipulado))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rechazaTokenExpirado() {
        JwtProperties corta = new JwtProperties();
        corta.setSecret(SECRETO_PRUEBA);
        corta.setAccessExpiration(Duration.ofMillis(1));
        JwtSupport efimero = new JwtSupport(corta);

        String token = efimero.generateAccessToken("laura@correo.com", 1L, "ESTUDIANTE");

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThatThrownBy(() -> efimero.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtProperties otraClave = new JwtProperties();
        otraClave.setSecret("b3N0cmEgY2xhdmUgc2VncXVpZGEgcGFyYSBsb3MgZGVtbyBlbnRlcnM=");
        JwtSupport distinto = new JwtSupport(otraClave);

        String token = distinto.generateAccessToken("laura@correo.com", 1L, "ADMIN");

        assertThatThrownBy(() -> jwtSupport.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}
