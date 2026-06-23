package Edusecurity.repository;

import Edusecurity.entity.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;


public interface LeccionRepository extends JpaRepository<Leccion, Long> {
}
