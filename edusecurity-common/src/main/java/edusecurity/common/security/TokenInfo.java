package edusecurity.common.security;

import java.util.List;

/**
 * Información del usuario autenticada extraída del JWT o de la clave de servicio interno.
 *
 * @param correo     correo electrónico del usuario (o "internal" para llamadas servicio a servicio)
 * @param usuarioId  identificador del usuario (puede ser null en llamadas internas)
 * @param rol        rol del usuario: ADMIN, ESTUDIANTE, GESTOR_CONTENIDO, SOPORTE o INTERNAL
 * @param tipo       tipo de token: access, refresh o service-key
 * @param permisos   permisos del rol en el momento de emitir el token (ver {@link Permisos})
 * @param mfa        true si el usuario tiene activada la verificación en dos pasos (HU25)
 */
public record TokenInfo(String correo, Long usuarioId, String rol, String tipo, List<String> permisos, boolean mfa) {

    public TokenInfo {
        permisos = (permisos == null) ? List.of() : List.copyOf(permisos);
    }

    /** Sin permisos ni MFA: llamadas internas y tokens de refresco. */
    public TokenInfo(String correo, Long usuarioId, String rol, String tipo) {
        this(correo, usuarioId, rol, tipo, List.of(), false);
    }

    public boolean esInterno() {
        return "INTERNAL".equals(rol);
    }

    public boolean tienePermiso(String permiso) {
        return permisos.contains(permiso);
    }
}
