package edusecurity.gateway.web;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS del backend: solo el gateway lo aplica (los microservicios no son accesibles desde el
 * navegador). Orígenes explícitos de {@code CORS_ORIGENES}, con credenciales; nunca "*".
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter(@Value("${app.gateway.cors-origenes}") String origenes) {
        List<String> limpios = Arrays.stream(origenes.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (limpios.isEmpty() || limpios.stream().anyMatch(o -> o.contains("*"))) {
            throw new IllegalStateException(
                    "CORS_ORIGENES debe listar orígenes explícitos (sin '*'), porque se permiten credenciales");
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(limpios);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
