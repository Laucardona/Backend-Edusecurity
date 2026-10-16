package edusecurity.simulacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de simulaciones: laboratorios de phishing con bandeja simulada (HU43).
 * Base de datos propia: edusecurity_simulacion.
 */
@SpringBootApplication
public class SimulacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulacionApplication.class, args);
    }
}
