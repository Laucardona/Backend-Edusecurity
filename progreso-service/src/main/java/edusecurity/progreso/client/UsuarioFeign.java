package edusecurity.progreso.client;

/**
 * Proyección mínima del usuario retornada por el servicio de autenticación.
 */
public record UsuarioFeign(Long id, String nombre, String correo, Boolean activo) {
}
