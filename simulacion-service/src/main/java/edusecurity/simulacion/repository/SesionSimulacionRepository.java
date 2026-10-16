package edusecurity.simulacion.repository;

import edusecurity.simulacion.entity.EstadoSesion;
import edusecurity.simulacion.entity.SesionSimulacion;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionSimulacionRepository extends JpaRepository<SesionSimulacion, Long> {

    Optional<SesionSimulacion> findByUuid(String uuid);

    Optional<SesionSimulacion> findFirstByEscenarioIdAndUsuarioIdAndEstadoOrderByIniciadaEnDesc(
            Long escenarioId, Long usuarioId, EstadoSesion estado);

    Page<SesionSimulacion> findByUsuarioId(Long usuarioId, Pageable pageable);
}
