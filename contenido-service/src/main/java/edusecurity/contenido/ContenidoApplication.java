package edusecurity.contenido;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de contenido educativo: módulos y lecciones.
 * Base de datos propia: edusecurity_contenido.
 */
@SpringBootApplication
public class ContenidoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContenidoApplication.class, args);
    }
}
