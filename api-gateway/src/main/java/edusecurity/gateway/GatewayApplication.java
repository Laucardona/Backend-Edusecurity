package edusecurity.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: punto único de entrada (puerto 8080).
 * Valida el JWT, aplica CORS, enruta vía Eureka y protege con Circuit Breaker.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
