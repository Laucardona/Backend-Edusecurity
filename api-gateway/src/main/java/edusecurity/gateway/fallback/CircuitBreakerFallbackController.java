package edusecurity.gateway.fallback;

import edusecurity.gateway.web.CorrelationIdGlobalFilter;
import edusecurity.gateway.web.ProblemaGateway;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Respuesta del Circuit Breaker cuando un microservicio no responde, en formato RFC 9457.
 * Invocado por el filtro CircuitBreaker de cada ruta.
 */
@RestController
public class CircuitBreakerFallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, Object>>> auth(ServerWebExchange exchange) {
        return responder(exchange, "auth-service");
    }

    @RequestMapping("/fallback/contenido")
    public Mono<ResponseEntity<Map<String, Object>>> contenido(ServerWebExchange exchange) {
        return responder(exchange, "contenido-service");
    }

    @RequestMapping("/fallback/progreso")
    public Mono<ResponseEntity<Map<String, Object>>> progreso(ServerWebExchange exchange) {
        return responder(exchange, "progreso-service");
    }

    @RequestMapping("/fallback/simulacion")
    public Mono<ResponseEntity<Map<String, Object>>> simulacion(ServerWebExchange exchange) {
        return responder(exchange, "simulacion-service");
    }

    private Mono<ResponseEntity<Map<String, Object>>> responder(ServerWebExchange exchange, String servicio) {
        Map<String, Object> body = ProblemaGateway.cuerpo(HttpStatus.SERVICE_UNAVAILABLE, "servicio-no-disponible",
                "Servicio no disponible",
                "El servicio " + servicio + " no está disponible en este momento. Intente de nuevo.",
                null, CorrelationIdGlobalFilter.de(exchange));
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body));
    }
}
