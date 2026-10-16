package edusecurity.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de seguridad HTTP del filtro JWT. CORS solo se configura en el gateway.
 */
@ConfigurationProperties(prefix = "app.security")
public class ApiSecurityProperties {

    /** Rutas públicas (sin token), acepta patrones tipo /swagger-ui/**. */
    private List<String> permitAll = new ArrayList<>();

    /** Clave compartida para llamadas servicio a servicio (cabecera X-Service-Key). */
    private String internalKey = "";

    /** Habilita el filtro de seguridad JWT. */
    private boolean enabled = false;

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll;
    }

    public String getInternalKey() {
        return internalKey;
    }

    public void setInternalKey(String internalKey) {
        this.internalKey = internalKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
