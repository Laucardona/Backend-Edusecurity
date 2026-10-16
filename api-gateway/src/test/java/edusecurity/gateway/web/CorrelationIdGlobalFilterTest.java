package edusecurity.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filtro = new CorrelationIdGlobalFilter();

    private ServerWebExchange ejecutar(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> reenviado = new AtomicReference<>();
        GatewayFilterChain chain = e -> {
            reenviado.set(e);
            return Mono.empty();
        };
        filtro.filter(exchange, chain).block();
        return reenviado.get();
    }

    @Test
    void sinCabeceraGeneraUnIdYLoReenviaYDevuelve() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/modulos"));

        ServerWebExchange reenviado = ejecutar(exchange);

        String id = reenviado.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER);
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER)).isEqualTo(id);
        assertThat(CorrelationIdGlobalFilter.de(exchange)).isEqualTo(id);
    }

    @Test
    void conservaUnIdValidoYReemplazaUnoInseguro() {
        MockServerWebExchange valido = MockServerWebExchange.from(MockServerHttpRequest.get("/api/x")
                .header(CorrelationIdGlobalFilter.HEADER, "pedido-42"));
        assertThat(ejecutar(valido).getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER))
                .isEqualTo("pedido-42");

        MockServerWebExchange inseguro = MockServerWebExchange.from(MockServerHttpRequest.get("/api/x")
                .header(CorrelationIdGlobalFilter.HEADER, "a".repeat(80)));
        String id = ejecutar(inseguro).getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER);
        assertThat(UUID.fromString(id)).isNotNull();
    }
}
