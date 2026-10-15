package edusecurity.auth.dto;

import edusecurity.auth.entity.Usuario;

/**
 * Respuesta de usuario: nunca incluye la contraseña.
 */
public record UsuarioResponse(Long id, String nombre, String correo, Boolean activo, RolResponse rol) {

    public static UsuarioResponse from(Usuario usuario) {
        RolResponse rol = (usuario.getRol() == null) ? null : RolResponse.from(usuario.getRol());
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getActivo(), rol);
    }
}
