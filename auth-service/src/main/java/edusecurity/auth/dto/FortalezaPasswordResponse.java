package edusecurity.auth.dto;

import java.util.List;

/**
 * Indicador de fortaleza (HU36): puntaje de 0 a 4, nivel DEBIL, MEDIA o FUERTE,
 * si cumple la política y qué le falta.
 */
public record FortalezaPasswordResponse(int puntaje, String nivel, boolean cumplePolitica,
                                        List<String> faltantes) {
}
