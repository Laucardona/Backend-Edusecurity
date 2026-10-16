package edusecurity.simulacion.repository;

import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.Nivel;
import edusecurity.simulacion.entity.TipoEscenario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EscenarioRepository extends JpaRepository<Escenario, Long> {

    Optional<Escenario> findByUuid(String uuid);

    @Query("""
            SELECT e FROM Escenario e
            WHERE (:tipo IS NULL OR e.tipo = :tipo)
              AND (:nivel IS NULL OR e.nivel = :nivel)
              AND (:soloPublicados = false OR e.publicado = true)
            """)
    Page<Escenario> buscar(@Param("tipo") TipoEscenario tipo,
                           @Param("nivel") Nivel nivel,
                           @Param("soloPublicados") boolean soloPublicados,
                           Pageable pageable);

    /** Cuenta los mensajes por escenario en una sola consulta (evita N+1). */
    @Query("SELECT m.escenario.id, COUNT(m) FROM MensajeSimulado m WHERE m.escenario.id IN :ids GROUP BY m.escenario.id")
    List<Object[]> contarMensajes(@Param("ids") List<Long> ids);
}
