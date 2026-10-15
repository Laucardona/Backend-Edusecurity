package edusecurity.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global del Gateway:
 * <ul>
 *   <li>Permite las rutas públicas (login, registro, swagger, salud, docs).</li>
 *   <li>Exige un access token JWT válido para todo lo demás.</li>
 *   <li>Devuelve errores JSON con el mismo formato que los microservicios.</li>
 * </ul>
 */
@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;
    private final List<String> publicPaths;

    public JwtGlobalFilter(JwtValidator jwtValidator, ObjectMapper objectMapper,
                           @Value("${app.gateway.public-paths}") List<String> publicPaths) {
        this.jwtValidator = jwtValidator;
        this.objectMapper = objectMapper;
        this.publicPaths = publicPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (esPublica(path)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return rechazar(exchange, "Se requiere un token JWT");
        }

        String token = authorization.substring(7);
        if (!jwtValidator.esAccessTokenValido(token)) {
            return rechazar(exchange, "Token inválido o expirado");
        }

        return chain.filter(exchange);
    }

    private boolean esPublica(String path) {
        return publicPaths.stream().anyMatch(ruta -> coincide(ruta, path));
    }

    /** Coincidencia simple: el patrón terminado en /** cubre todos los hijos. */
    private boolean coincide(String patron, String path) {
        if (patron.endsWith("/**")) {
            String base = patron.substring(0, patron.length() - 3);
            return path.equals(base) || path.startsWith(base + "/");
        }
        if (patron.endsWith("/*")) {
            String base = patron.substring(0, patron.length() - 2);
            String resto = path.startsWith(base) ? path.substring(base.length()) : "";
            return !resto.contains("/");
        }
        return path.equals(patron);
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", mensaje);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("fieldErrors", List.of());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            bytes = "{\"status\":401,\"message\":\"No autorizado\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // Antes del enrutado y de los filtros de ruta
        return -100;
    }
}
