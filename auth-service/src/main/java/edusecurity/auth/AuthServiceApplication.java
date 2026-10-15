package edusecurity.auth;

import org.springframework.boot.SpringApplication;
import edusecurity.auth.config.AccesoProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio de autenticación: registro, login, refresh token, usuarios y roles.
 * Base de datos propia: edusecurity_auth.
 */
@SpringBootApplication
@EnableConfigurationProperties(AccesoProperties.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
