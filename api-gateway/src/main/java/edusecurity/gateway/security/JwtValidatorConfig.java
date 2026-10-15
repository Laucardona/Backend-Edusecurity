package edusecurity.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtValidatorConfig {

    @Bean
    public JwtValidator jwtValidator(@Value("${app.jwt.secret:}") String secret) {
        return new JwtValidator(secret);
    }
}
