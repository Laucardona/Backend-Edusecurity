package edusecurity.auth.dto;

import edusecurity.auth.entity.Usuario;
import java.time.Instant;

/**
 * Respuesta de usuario: nunca incluye la contraseña.
 */
public record UsuarioResponse(Long id, String nombre, String correo, Boolean activo, RolResponse rol,
                              Instant bloqueadoHasta) {

    public static UsuarioResponse from(Usuario usuario) {
        RolResponse rol = (usuario.getRol() == null) ? null : RolResponse.from(usuario.getRol());
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getActivo(), rol, usuario.getBloqueadoHasta());
    }
}
