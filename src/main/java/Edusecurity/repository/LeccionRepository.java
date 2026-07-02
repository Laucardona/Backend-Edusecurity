package Edusecurity.repository;

import Edusecurity.entity.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    List<Leccion> findByModuloId(Long moduloId);

}