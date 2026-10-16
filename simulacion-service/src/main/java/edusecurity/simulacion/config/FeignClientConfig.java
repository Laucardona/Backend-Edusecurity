package edusecurity.simulacion.config;

import edusecurity.common.web.CorrelationId;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cabeceras de todas las llamadas Feign: X-Service-Key para que los servicios destino
 * autentiquen la llamada interna (rol INTERNAL) y X-Correlation-Id para seguir la petición
 * original en los logs de todos los servicios.
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

    /** Propaga el id de correlación del MDC. Fuera de una petición (tareas programadas) no hay ninguno. */
    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String id = MDC.get(CorrelationId.MDC_KEY);
            if (id != null) {
                template.header(CorrelationId.HEADER, id);
            }
        };
    }
}
