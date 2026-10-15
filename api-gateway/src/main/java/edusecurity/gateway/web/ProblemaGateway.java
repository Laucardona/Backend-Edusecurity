package edusecurity.gateway.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Errores que genera el propio gateway, en el mismo formato RFC 9457 que los microservicios
 * ({@code application/problem+json} con type, title, status, detail, instance, correlationId y
 * timestamp).
 */
public final class ProblemaGateway {

    private ProblemaGateway() {
    }

    public static Map<String, Object> cuerpo(HttpStatus status, String tipo, String titulo, String detalle,
                                             String instancia, String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "urn:edusecurity:problema:" + tipo);
        body.put("title", titulo);
        body.put("status", status.value());
        body.put("detail", detalle);
        if (instancia != null) {
            body.put("instance", instancia);
        }
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    public static Mono<Void> escribir(ServerWebExchange exchange, ObjectMapper objectMapper, HttpStatus status,
                                      String tipo, String titulo, String detalle) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");

        Map<String, Object> body = cuerpo(status, tipo, titulo, detalle,
                exchange.getRequest().getPath().value(), CorrelationIdGlobalFilter.de(exchange));
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            bytes = ("{\"status\":" + status.value() + "}").getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
