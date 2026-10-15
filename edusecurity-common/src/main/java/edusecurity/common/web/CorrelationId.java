package edusecurity.common.web;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identificador de correlación de una petición: viaja en la cabecera {@value #HEADER}, se guarda
 * en el MDC con la clave {@value #MDC_KEY} (aparece en cada línea de log) y se devuelve en la
 * respuesta y en los errores.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /** Un UUID o un token seguro: letras, dígitos, punto, guion y guion bajo, hasta 64 caracteres. */
    private static final Pattern SEGURO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private CorrelationId() {
    }

    /** Devuelve el valor recibido si es seguro; si no (ausente, largo o con caracteres raros), uno nuevo. */
    public static String validarOGenerar(String recibido) {
        if (recibido != null && SEGURO.matcher(recibido).matches()) {
            return recibido;
        }
        return UUID.randomUUID().toString();
    }
}
