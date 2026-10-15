package edusecurity.progreso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microservicio de progreso de estudiantes. Consulta a auth-service y
 * contenido-service mediante Feign para denormalizar sus respuestas.
 * Base de datos propia: edusecurity_progreso.
 */
@SpringBootApplication
@EnableFeignClients
public class ProgresoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProgresoApplication.class, args);
    }
}
