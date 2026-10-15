package edusecurity.contenido.repository;

import edusecurity.contenido.entity.Modulo;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    @Query("""
            SELECT m FROM Modulo m
            WHERE (:q IS NULL OR LOWER(m.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(m.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:soloActivos = false OR m.activo = true)
            """)
    Page<Modulo> buscar(@Param("q") String q,
                        @Param("soloActivos") boolean soloActivos,
                        Pageable pageable);

    /** Cuenta lecciones por módulo en una sola consulta (evita N+1). */
    @Query("SELECT l.modulo.id, COUNT(l) FROM Leccion l WHERE l.modulo.id IN :ids GROUP BY l.modulo.id")
    List<Object[]> contarLeccionesPorModulo(@Param("ids") List<Long> ids);
}
