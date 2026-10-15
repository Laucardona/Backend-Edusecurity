package edusecurity.common.security;

/**
 * Información del usuario autenticada extraída del JWT o de la clave de servicio interno.
 *
 * @param correo     correo electrónico del usuario (o "internal" para llamadas servicio a servicio)
 * @param usuarioId  identificador del usuario (puede ser null en llamadas internas)
 * @param rol        rol del usuario: ADMIN, ESTUDIANTE o INTERNAL
 * @param tipo       tipo de token: access, refresh o service-key
 */
public record TokenInfo(String correo, Long usuarioId, String rol, String tipo) {

    public boolean esInterno() {
        return "INTERNAL".equals(rol);
    }
}
