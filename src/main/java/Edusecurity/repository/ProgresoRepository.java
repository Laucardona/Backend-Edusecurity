package Edusecurity.repository;

import Edusecurity.entity.ProgresoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgresoRepository extends JpaRepository<ProgresoUsuario, Long> {

    List<ProgresoUsuario> findByUsuarioId(Long usuarioId);

}