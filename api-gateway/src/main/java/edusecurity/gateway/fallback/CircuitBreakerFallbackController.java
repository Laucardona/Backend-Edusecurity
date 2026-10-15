package edusecurity.gateway.fallback;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Respuesta del Circuit Breaker cuando un microservicio no responde.
 * Invocado por el filtro CircuitBreaker de cada ruta.
 */
@RestController
public class CircuitBreakerFallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, Object>>> auth() {
        return responder("auth-service");
    }

    @RequestMapping("/fallback/contenido")
    public Mono<ResponseEntity<Map<String, Object>>> contenido() {
        return responder("contenido-service");
    }

    @RequestMapping("/fallback/progreso")
    public Mono<ResponseEntity<Map<String, Object>>> progreso() {
        return responder("progreso-service");
    }

    private Mono<ResponseEntity<Map<String, Object>>> responder(String servicio) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 503,
                "error", "Service Unavailable",
                "message", "El servicio " + servicio + " no está disponible en este momento. Intente de nuevo.",
                "path", "",
                "fieldErrors", List.of());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
