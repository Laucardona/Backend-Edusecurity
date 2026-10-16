package edusecurity.simulacion.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agrega la cabecera X-Service-Key a todas las llamadas Feign para que los
 * servicios destino autentiquen las llamadas internas (rol INTERNAL).
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor internalServiceKeyInterceptor(
            @Value("${app.security.internal-key:}") String internalKey) {

        return template -> {
            if (internalKey != null && !internalKey.isBlank()) {
                template.header("X-Service-Key", internalKey);
            }
        };
    }
}
