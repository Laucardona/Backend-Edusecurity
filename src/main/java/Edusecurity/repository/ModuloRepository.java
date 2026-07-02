package Edusecurity.repository;

import Edusecurity.entity.ModuloEducativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuloRepository extends JpaRepository<ModuloEducativo, Long> {

}