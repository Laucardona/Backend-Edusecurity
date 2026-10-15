package edusecurity.simulacion.entity;

public enum EstadoSesion {
    EN_CURSO,
    FINALIZADA,
    /** Se acabó el tiempo antes de que el estudiante finalizara. */
    VENCIDA
}
