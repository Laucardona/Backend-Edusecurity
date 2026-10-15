package edusecurity.auth.repository;

import edusecurity.auth.entity.TokenRecuperacion;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Invalida los tokens pendientes del usuario: solo el último enviado sirve. */
    @Modifying
    @Query("""
            UPDATE TokenRecuperacion t SET t.usadoEn = :ahora
            WHERE t.usuario.id = :usuarioId AND t.usadoEn IS NULL
            """)
    int invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);
}
