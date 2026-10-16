package edusecurity.common.error;

import edusecurity.common.web.CorrelationId;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Errores en formato RFC 9457 ({@code application/problem+json}) para todos los servicios:
 * {@code type}, {@code title}, {@code status}, {@code detail} e {@code instance}, más las propiedades
 * {@code correlationId}, {@code timestamp} y, en errores de validación, {@code errores}
 * (campo → mensaje).
 */
public final class Problemas {

    public static final String PREFIJO_TIPO = "urn:edusecurity:problema:";

    private Problemas() {
    }

    public static ProblemDetail crear(HttpStatus status, String detalle, String instancia) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setType(URI.create(PREFIJO_TIPO + tipo(status)));
        problema.setTitle(titulo(status));
        if (instancia != null) {
            problema.setInstance(URI.create(instancia));
        }
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }
        problema.setProperty("timestamp", Instant.now().toString());
        return problema;
    }

    public static ProblemDetail validacion(String detalle, String instancia, Map<String, String> errores) {
        ProblemDetail problema = crear(HttpStatus.BAD_REQUEST, detalle, instancia);
        problema.setProperty("errores", errores);
        return problema;
    }

    static String tipo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "solicitud-invalida";
            case UNAUTHORIZED -> "no-autenticado";
            case FORBIDDEN -> "sin-permiso";
            case NOT_FOUND -> "no-encontrado";
            case METHOD_NOT_ALLOWED -> "metodo-no-permitido";
            case CONFLICT -> "conflicto";
            case LOCKED -> "cuenta-bloqueada";
            case SERVICE_UNAVAILABLE -> "servicio-no-disponible";
            case INTERNAL_SERVER_ERROR -> "error-interno";
            default -> String.valueOf(status.value());
        };
    }

    static String titulo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Solicitud inválida";
            case UNAUTHORIZED -> "No autenticado";
            case FORBIDDEN -> "Sin permiso";
            case NOT_FOUND -> "Recurso no encontrado";
            case METHOD_NOT_ALLOWED -> "Método no permitido";
            case CONFLICT -> "Conflicto con el estado actual";
            case LOCKED -> "Cuenta bloqueada";
            case SERVICE_UNAVAILABLE -> "Servicio no disponible";
            case INTERNAL_SERVER_ERROR -> "Error interno";
            default -> status.getReasonPhrase();
        };
    }
}
