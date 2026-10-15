package edusecurity.auth.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Política de contraseñas seguras (HU36): longitud mínima, mayúsculas, minúsculas,
 * números y caracteres especiales. También calcula el indicador de fortaleza.
 */
public final class PoliticaPassword {

    public static final int LONGITUD_MINIMA = 10;
    /** Límite de BCrypt: solo usa los primeros 72 bytes. */
    public static final int LONGITUD_MAXIMA = 72;

    private PoliticaPassword() {
    }

    /** Lista de requisitos que la contraseña no cumple (vacía si cumple la política). */
    public static List<String> requisitosFaltantes(String password) {
        List<String> faltantes = new ArrayList<>();
        String valor = (password == null) ? "" : password;

        if (valor.length() < LONGITUD_MINIMA) {
            faltantes.add("al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (valor.length() > LONGITUD_MAXIMA) {
            faltantes.add("máximo " + LONGITUD_MAXIMA + " caracteres");
        }
        if (valor.chars().noneMatch(Character::isUpperCase)) {
            faltantes.add("una letra mayúscula");
        }
        if (valor.chars().noneMatch(Character::isLowerCase)) {
            faltantes.add("una letra minúscula");
        }
        if (valor.chars().noneMatch(Character::isDigit)) {
            faltantes.add("un número");
        }
        if (valor.chars().allMatch(Character::isLetterOrDigit)) {
            faltantes.add("un carácter especial");
        }
        if (!valor.isEmpty() && valor.isBlank()) {
            faltantes.add("caracteres distintos de espacios");
        }
        return faltantes;
    }

    public static boolean cumple(String password) {
        return requisitosFaltantes(password).isEmpty();
    }

    /**
     * Puntaje de 0 a 4 para el indicador de fortaleza: 4 si cumple la política y tiene
     * 12 o más caracteres, 3 si solo cumple la política, y menos por cada requisito que falte.
     */
    public static int puntaje(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        int faltantes = requisitosFaltantes(password).size();
        if (faltantes == 0) {
            return (password.length() >= 12) ? 4 : 3;
        }
        return Math.max(0, 3 - faltantes);
    }

    public static String nivel(int puntaje) {
        if (puntaje >= 4) {
            return "FUERTE";
        }
        if (puntaje >= 2) {
            return "MEDIA";
        }
        return "DEBIL";
    }
}
