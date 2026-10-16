package edusecurity.simulacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio de simulaciones: laboratorios de phishing con bandeja simulada (HU43) y
 * laboratorios guiados por pasos (HU44). Envía los resultados a progreso-service (I7.4).
 * Base de datos propia: edusecurity_simulacion.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class SimulacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulacionApplication.class, args);
    }
}
