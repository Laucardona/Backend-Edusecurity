package edusecurity.auth.repository;

import edusecurity.auth.entity.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    /** Roles con sus permisos cargados en una sola consulta. */
    @EntityGraph(attributePaths = "permisos")
    List<Rol> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "permisos")
    Optional<Rol> findWithPermisosById(Long id);
}
