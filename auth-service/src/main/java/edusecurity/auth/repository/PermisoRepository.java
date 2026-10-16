package edusecurity.auth.repository;

import edusecurity.auth.entity.Permiso;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findByCodigoIn(Collection<String> codigos);

    List<Permiso> findAllByOrderByCodigoAsc();
}
