package edusecurity.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Sesión iniciada por un usuario (HU3, HU37). Guarda el hash del refresh token vigente,
 * que cambia en cada rotación; al cerrar sesión se marca {@code revocadaEn}.
 */
@Entity
@Table(name = "sesiones")
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(length = 45)
    private String ip;

    @Column(length = 255)
    private String dispositivo;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "ultimo_uso", nullable = false)
    private Instant ultimoUso;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocada_en")
    private Instant revocadaEn;

    public boolean estaActiva(Instant ahora) {
        return revocadaEn == null && expiraEn.isAfter(ahora);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(Instant creadaEn) {
        this.creadaEn = creadaEn;
    }

    public Instant getUltimoUso() {
        return ultimoUso;
    }

    public void setUltimoUso(Instant ultimoUso) {
        this.ultimoUso = ultimoUso;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(Instant expiraEn) {
        this.expiraEn = expiraEn;
    }

    public Instant getRevocadaEn() {
        return revocadaEn;
    }

    public void setRevocadaEn(Instant revocadaEn) {
        this.revocadaEn = revocadaEn;
    }
}
