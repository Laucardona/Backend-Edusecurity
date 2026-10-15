package edusecurity.auth.repository;

import edusecurity.auth.entity.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:q IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Usuario> buscar(@Param("q") String q, Pageable pageable);

    // ------------------------------------------------------------------ métricas (HU33)

    long countByActivoTrue();

    long countByBloqueadoHastaAfter(Instant ahora);

    /** [nombre del rol (null si no tiene), cantidad de usuarios]. */
    @Query("SELECT r.nombre, COUNT(u) FROM Usuario u LEFT JOIN u.rol r GROUP BY r.nombre")
    List<Object[]> contarPorRol();
}
