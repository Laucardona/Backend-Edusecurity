package edusecurity.auth.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Métricas de usuarios para el panel administrativo (HU33).
 * Los usuarios no guardan fecha de creación, así que no hay "nuevos en el periodo".
 *
 * @param porRol                     nombre del rol → cantidad de usuarios ("SIN_ROL" si no tienen)
 * @param bloqueadosAhora            usuarios con bloqueo temporal vigente
 * @param usuariosConSesionEnPeriodo usuarios distintos que iniciaron sesión entre desde y hasta
 * @param iniciosSesionEnPeriodo     inicios de sesión entre desde y hasta
 */
public record MetricasUsuariosResponse(
        LocalDate desde,
        LocalDate hasta,
        long totalUsuarios,
        long usuariosActivos,
        Map<String, Long> porRol,
        long bloqueadosAhora,
        long usuariosConSesionEnPeriodo,
        long iniciosSesionEnPeriodo) {
}
