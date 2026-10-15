package edusecurity.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades JWT compartidas por el API Gateway, el servicio de autenticación
 * y todos los microservicios que validan tokens.
 *
 * <p>El secreto no tiene valor por defecto: se recibe por la variable de entorno {@code JWT_SECRET}.</p>
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Clave secreta en Base64 usada para firmar los tokens (mínimo 32 bytes). */
    private String secret = "";

    /** Vigencia del token de acceso. */
    private Duration accessExpiration = Duration.ofHours(10);

    /** Vigencia del token de refresco. */
    private Duration refreshExpiration = Duration.ofDays(7);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessExpiration() {
        return accessExpiration;
    }

    public void setAccessExpiration(Duration accessExpiration) {
        this.accessExpiration = accessExpiration;
    }

    public Duration getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(Duration refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
