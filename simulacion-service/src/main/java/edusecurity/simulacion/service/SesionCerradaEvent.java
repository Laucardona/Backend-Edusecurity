package edusecurity.simulacion.service;

/** Una sesión pasó a FINALIZADA o VENCIDA. Se procesa después del commit (I7.4). */
public record SesionCerradaEvent(Long sesionId) {
}
