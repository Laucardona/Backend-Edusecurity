package edusecurity.auth.repository;

import edusecurity.auth.entity.Sesion;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByRefreshTokenHash(String refreshTokenHash);

    Page<Sesion> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Sesion s SET s.revocadaEn = :ahora
            WHERE s.usuario.id = :usuarioId AND s.revocadaEn IS NULL
            """)
    int revocarTodas(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

    // ------------------------------------------------------------------ métricas (HU33)

    /** Inicios de sesión con creadaEn en [inicio, fin). */
    @Query("SELECT COUNT(s) FROM Sesion s WHERE s.creadaEn >= :inicio AND s.creadaEn < :fin")
    long contarCreadasEntre(@Param("inicio") Instant inicio, @Param("fin") Instant fin);

    /** Usuarios distintos que iniciaron sesión en [inicio, fin). */
    @Query("SELECT COUNT(DISTINCT s.usuario.id) FROM Sesion s WHERE s.creadaEn >= :inicio AND s.creadaEn < :fin")
    long contarUsuariosConSesionEntre(@Param("inicio") Instant inicio, @Param("fin") Instant fin);
}
