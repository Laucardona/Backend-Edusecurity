package edusecurity.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class EncabezadosInternosGlobalFilterTest {

    private final EncabezadosInternosGlobalFilter filtro = new EncabezadosInternosGlobalFilter();

    @Test
    void quitaXServiceKeyYCabecerasXUserDelClienteYConservaLasDemas() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/progreso/mio")
                .header("X-Service-Key", "robada")
                .header("x-service-key", "otra")
                .header("X-User-Id", "1")
                .header("X-USER-ROL", "ADMIN")
                .header(HttpHeaders.AUTHORIZATION, "Bearer abc")
                .header("X-Correlation-Id", "pedido-1"));

        AtomicReference<ServerWebExchange> reenviado = new AtomicReference<>();
        GatewayFilterChain chain = e -> {
            reenviado.set(e);
            return Mono.empty();
        };
        filtro.filter(exchange, chain).block();

        HttpHeaders headers = reenviado.get().getRequest().getHeaders();
        assertThat(headers.containsKey("X-Service-Key")).isFalse();
        assertThat(headers.containsKey("X-User-Id")).isFalse();
        assertThat(headers.containsKey("X-USER-ROL")).isFalse();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer abc");
        assertThat(headers.getFirst("X-Correlation-Id")).isEqualTo("pedido-1");
    }
}
