package edusecurity.progreso.repository;

import edusecurity.progreso.entity.SimulacionCompletada;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulacionCompletadaRepository extends JpaRepository<SimulacionCompletada, Long> {

    Optional<SimulacionCompletada> findBySesionUuid(String sesionUuid);

    Page<SimulacionCompletada> findByUsuarioId(Long usuarioId, Pageable pageable);

    long countByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndAprobadoTrue(Long usuarioId);
}
