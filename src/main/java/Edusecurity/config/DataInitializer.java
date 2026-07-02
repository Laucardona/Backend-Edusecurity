package Edusecurity.config;

import Edusecurity.entity.Rol;
import Edusecurity.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository repository;

    public DataInitializer(RolRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {

        if (repository.findByNombre("ADMIN").isEmpty()) {

            Rol admin = new Rol();
            admin.setNombre("ADMIN");

            repository.save(admin);

        }

        if (repository.findByNombre("ESTUDIANTE").isEmpty()) {

            Rol estudiante = new Rol();
            estudiante.setNombre("ESTUDIANTE");

            repository.save(estudiante);

        }

    }

}   