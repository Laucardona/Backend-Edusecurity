package edusecurity.auth.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Datos del cliente que inicia sesión, para el historial de sesiones (HU37).
 */
public record ClienteInfo(String ip, String dispositivo) {

    public static ClienteInfo from(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        String ip = (reenviada != null && !reenviada.isBlank())
                ? reenviada.split(",")[0].trim()
                : request.getRemoteAddr();
        return new ClienteInfo(recortar(ip, 45), recortar(request.getHeader("User-Agent"), 255));
    }

    private static String recortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return (valor.length() <= max) ? valor : valor.substring(0, max);
    }
}
