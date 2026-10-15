package edusecurity.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de acceso de M01 configurables por ambiente ({@code app.acceso.*}).
 * Los valores por defecto son provisionales hasta que el equipo los confirme.
 */
@ConfigurationProperties(prefix = "app.acceso")
public class AccesoProperties {

    /** Intentos fallidos seguidos que bloquean la cuenta (HU38). */
    private int maxIntentosFallidos = 5;

    /** Tiempo que dura el bloqueo temporal (HU38). */
    private Duration duracionBloqueo = Duration.ofMinutes(15);

    /** Vigencia del enlace de recuperación de contraseña (HU4). */
    private Duration vigenciaRecuperacion = Duration.ofMinutes(30);

    /** URL del frontend donde el usuario escribe la nueva contraseña; se le agrega ?token=... */
    private String urlRestablecer = "http://localhost:5500/restablecer.html";

    /** Remitente de los correos del sistema. */
    private String correoRemitente = "no-reply@edusecurity.com";

    public int getMaxIntentosFallidos() {
        return maxIntentosFallidos;
    }

    public void setMaxIntentosFallidos(int maxIntentosFallidos) {
        this.maxIntentosFallidos = maxIntentosFallidos;
    }

    public Duration getDuracionBloqueo() {
        return duracionBloqueo;
    }

    public void setDuracionBloqueo(Duration duracionBloqueo) {
        this.duracionBloqueo = duracionBloqueo;
    }

    public Duration getVigenciaRecuperacion() {
        return vigenciaRecuperacion;
    }

    public void setVigenciaRecuperacion(Duration vigenciaRecuperacion) {
        this.vigenciaRecuperacion = vigenciaRecuperacion;
    }

    public String getUrlRestablecer() {
        return urlRestablecer;
    }

    public void setUrlRestablecer(String urlRestablecer) {
        this.urlRestablecer = urlRestablecer;
    }

    public String getCorreoRemitente() {
        return correoRemitente;
    }

    public void setCorreoRemitente(String correoRemitente) {
        this.correoRemitente = correoRemitente;
    }
}
