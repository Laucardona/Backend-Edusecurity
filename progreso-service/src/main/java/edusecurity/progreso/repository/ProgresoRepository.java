package edusecurity.progreso.repository;

import edusecurity.progreso.entity.Progreso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgresoRepository extends JpaRepository<Progreso, Long> {

    Optional<Progreso> findByUsuarioIdAndLeccionId(Long usuarioId, Long leccionId);

    List<Progreso> findByUsuarioId(Long usuarioId);

    List<Progreso> findByLeccionId(Long leccionId);

    @Query("""
            SELECT p FROM Progreso p
            WHERE (:usuarioId IS NULL OR p.usuarioId = :usuarioId)
              AND (:leccionId IS NULL OR p.leccionId = :leccionId)
              AND (:moduloId IS NULL OR p.moduloId = :moduloId)
            """)
    Page<Progreso> buscar(@Param("usuarioId") Long usuarioId,
                          @Param("leccionId") Long leccionId,
                          @Param("moduloId") Long moduloId,
                          Pageable pageable);

    // ------------------------------------------------------------------ panel administrativo (HU33)

    /** Una fila: [promedio de porcentaje, lecciones completadas, estudiantes con progreso]. */
    @Query("""
            SELECT AVG(p.porcentaje), SUM(CASE WHEN p.completado = true THEN 1 ELSE 0 END), COUNT(DISTINCT p.usuarioId)
            FROM Progreso p
            """)
    List<Object[]> resumenGeneral();

    /** [moduloId, promedio de porcentaje, lecciones completadas] por módulo. */
    @Query("""
            SELECT p.moduloId, AVG(p.porcentaje), SUM(CASE WHEN p.completado = true THEN 1 ELSE 0 END)
            FROM Progreso p
            GROUP BY p.moduloId
            ORDER BY p.moduloId
            """)
    List<Object[]> resumenPorModulo();
}
