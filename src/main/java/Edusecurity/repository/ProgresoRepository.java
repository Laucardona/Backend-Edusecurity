package Edusecurity.repository;

import Edusecurity.entity.ProgresoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgresoRepository extends JpaRepository<ProgresoUsuario, Long> {
}
