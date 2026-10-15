package edusecurity.contenido.repository;

import edusecurity.contenido.entity.Leccion;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    Page<Leccion> findByModuloId(Long moduloId, Pageable pageable);

    List<Leccion> findByModuloIdOrderByOrdenAsc(Long moduloId);

    long countByModuloId(Long moduloId);

    @Modifying
    @Query("DELETE FROM Leccion l WHERE l.modulo.id = :moduloId")
    void eliminarPorModulo(@Param("moduloId") Long moduloId);

    @Query("""
            SELECT l FROM Leccion l
            WHERE (:q IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(l.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:moduloId IS NULL OR l.modulo.id = :moduloId)
            """)
    Page<Leccion> buscar(@Param("q") String q,
                         @Param("moduloId") Long moduloId,
                         Pageable pageable);
}
