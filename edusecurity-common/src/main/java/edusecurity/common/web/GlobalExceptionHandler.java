package edusecurity.common.web;

import edusecurity.common.error.Problemas;
import edusecurity.common.exception.AccountLockedException;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ForbiddenException;
import edusecurity.common.exception.InvalidTokenException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.exception.ServiceUnavailableException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejador global de excepciones: todos los servicios responden los errores en formato
 * RFC 9457 ({@code application/problem+json}, ver {@link Problemas}) en lugar de los HTML/JSON
 * por defecto. Un 500 nunca expone el mensaje de la excepción.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ProblemDetail> responder(ProblemDetail problema) {
        return ResponseEntity.status(problema.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    private static ResponseEntity<ProblemDetail> build(HttpServletRequest request, HttpStatus status, String detalle) {
        return responder(Problemas.crear(status, detalle, request == null ? null : request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> badRequest(BadRequestException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> conflict(ConflictException ex, HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> forbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> locked(AccountLockedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.LOCKED, ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> serviceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        log.warn("Servicio dependiente no disponible en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(request, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, JwtException.class})
    public ResponseEntity<ProblemDetail> invalidToken(RuntimeException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> authentication(AuthenticationException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNAUTHORIZED,
                (ex.getMessage() == null) ? "No autorizado" : ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String detalle = errores.isEmpty()
                ? "Datos inválidos"
                : errores.entrySet().iterator().next().getKey() + ": " + errores.values().iterator().next();
        return responder(Problemas.validacion(detalle, request.getRequestURI(), errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Cuerpo de la solicitud inválido o malformado");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> missingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Falta el parámetro requerido '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> methodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                            HttpServletRequest request) {
        return build(request, HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no soportado: " + ex.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    /** El mensaje de la excepción solo va al log (con el correlationId), nunca al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(request, HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor. Indica el correlationId si reportas el problema.");
    }
}
