package edusecurity.auth.dto;

import edusecurity.auth.entity.Rol;

public record RolResponse(Long id, String nombre, String descripcion) {

    public static RolResponse from(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion());
    }
}
