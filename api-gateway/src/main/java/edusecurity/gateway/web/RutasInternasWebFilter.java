package edusecurity.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Las rutas {@code /internal/**} son solo servicio a servicio: desde fuera responden 404 aunque
 * hoy ninguna ruta del gateway las enrute, para que nunca queden expuestas por error.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RutasInternasWebFilter implements WebFilter {

    private final ObjectMapper objectMapper;

    public RutasInternasWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (esInterna(exchange.getRequest().getPath().value())) {
            return ProblemaGateway.escribir(exchange, objectMapper, HttpStatus.NOT_FOUND,
                    "no-encontrado", "Recurso no encontrado", "Recurso no encontrado");
        }
        return chain.filter(exchange);
    }

    /** Ignora mayúsculas y barras repetidas: /INTERNAL/x y //internal/x también cuentan. */
    static boolean esInterna(String path) {
        String normalizado = path.replaceAll("/{2,}", "/").toLowerCase(Locale.ROOT);
        return normalizado.equals("/internal") || normalizado.startsWith("/internal/");
    }
}
