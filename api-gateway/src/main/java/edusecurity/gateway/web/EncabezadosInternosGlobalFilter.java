package edusecurity.gateway.web;

import java.util.List;
import java.util.Locale;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Quita las cabeceras que solo pueden poner los propios servicios antes de enrutar: un cliente
 * externo no puede hacerse pasar por un servicio interno ({@code X-Service-Key}) ni inyectar
 * datos de usuario ({@code X-User-*}).
 */
@Component
public class EncabezadosInternosGlobalFilter implements GlobalFilter, Ordered {

    static final String SERVICE_KEY = "X-Service-Key";
    static final String PREFIJO_USUARIO = "x-user-";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<String> aQuitar = exchange.getRequest().getHeaders().keySet().stream()
                .filter(EncabezadosInternosGlobalFilter::esInterna)
                .toList();
        if (aQuitar.isEmpty()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> aQuitar.forEach(h::remove))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    static boolean esInterna(String cabecera) {
        String nombre = cabecera.toLowerCase(Locale.ROOT);
        return nombre.equals(SERVICE_KEY.toLowerCase(Locale.ROOT)) || nombre.startsWith(PREFIJO_USUARIO);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
