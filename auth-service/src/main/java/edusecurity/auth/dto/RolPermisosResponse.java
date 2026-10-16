package edusecurity.auth.dto;

import edusecurity.auth.entity.Rol;
import java.util.List;

/** Rol con sus permisos (GET /api/roles, PUT /api/roles/{id}/permisos). */
public record RolPermisosResponse(Long id, String nombre, String descripcion, List<String> permisos) {

    public static RolPermisosResponse from(Rol rol) {
        return new RolPermisosResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.codigosPermisos());
    }
}
