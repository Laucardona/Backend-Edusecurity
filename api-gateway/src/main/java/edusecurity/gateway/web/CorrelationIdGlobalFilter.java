package edusecurity.gateway.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Primer filtro del gateway: garantiza un {@code X-Correlation-Id} en cada petición. Si el cliente
 * envía uno válido (UUID o token seguro de hasta 64 caracteres) se conserva; si no, se genera.
 * Se reenvía al microservicio y se devuelve en la respuesta. Mismas reglas que
 * {@code edusecurity.common.web.CorrelationId} en los servicios.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    /** Atributo del exchange con el id, para los errores que genera el propio gateway. */
    public static final String ATRIBUTO = CorrelationIdGlobalFilter.class.getName() + ".id";

    private static final Pattern SEGURO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = validarOGenerar(exchange.getRequest().getHeaders().getFirst(HEADER));
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, id))
                .build();
        exchange.getAttributes().put(ATRIBUTO, id);
        exchange.getResponse().getHeaders().set(HEADER, id);
        return chain.filter(exchange.mutate().request(request).build());
    }

    static String validarOGenerar(String recibido) {
        if (recibido != null && SEGURO.matcher(recibido).matches()) {
            return recibido;
        }
        return UUID.randomUUID().toString();
    }

    public static String de(ServerWebExchange exchange) {
        return exchange.getAttribute(ATRIBUTO);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
