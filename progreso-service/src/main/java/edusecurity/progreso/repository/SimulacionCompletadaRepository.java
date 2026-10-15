package edusecurity.progreso.repository;

import edusecurity.progreso.entity.SimulacionCompletada;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulacionCompletadaRepository extends JpaRepository<SimulacionCompletada, Long> {

    Optional<SimulacionCompletada> findBySesionUuid(String sesionUuid);

    Page<SimulacionCompletada> findByUsuarioId(Long usuarioId, Pageable pageable);

    long countByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndAprobadoTrue(Long usuarioId);

    // ------------------------------------------------------------------ panel administrativo (HU33)

    /** [tipo, completadas, aprobadas, suma de porcentajes] de las terminadas en [inicio, fin). */
    @Query("""
            SELECT s.tipo, COUNT(s), SUM(CASE WHEN s.aprobado = true THEN 1 ELSE 0 END), SUM(s.porcentaje)
            FROM SimulacionCompletada s
            WHERE s.finalizadaEn >= :inicio AND s.finalizadaEn < :fin
            GROUP BY s.tipo
            """)
    List<Object[]> resumenPorTipo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /** [escenarioUuid, titulo, completadas, aprobadas], de más a menos completadas, en [inicio, fin). */
    @Query("""
            SELECT s.escenarioUuid, s.titulo, COUNT(s), SUM(CASE WHEN s.aprobado = true THEN 1 ELSE 0 END)
            FROM SimulacionCompletada s
            WHERE s.finalizadaEn >= :inicio AND s.finalizadaEn < :fin
            GROUP BY s.escenarioUuid, s.titulo
            ORDER BY COUNT(s) DESC, s.titulo ASC
            """)
    List<Object[]> escenariosMasCompletados(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                            Pageable pageable);
}
