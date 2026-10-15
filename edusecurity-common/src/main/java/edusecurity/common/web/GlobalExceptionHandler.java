package edusecurity.common.web;

import edusecurity.common.error.ApiError;
import edusecurity.common.error.FieldErrorDetail;
import edusecurity.common.exception.BadRequestException;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ForbiddenException;
import edusecurity.common.exception.InvalidTokenException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.exception.ServiceUnavailableException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
 * Manejador global de excepciones: garantiza que todos los servicios respondan
 * con el mismo formato de error (ApiError) en lugar de los HTML/JSON por defecto.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> build(HttpServletRequest request, HttpStatus status, String message) {
        String path = (request == null) ? null : request.getRequestURI();
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, path));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiError> serviceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        log.warn("Servicio dependiente no disponible en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(request, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, JwtException.class})
    public ResponseEntity<ApiError> invalidToken(RuntimeException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNAUTHORIZED,
                (ex.getMessage() == null) ? "No autorizado" : ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        String message = detalles.isEmpty()
                ? "Datos inválidos"
                : detalles.get(0).field() + ": " + detalles.get(0).message();
        String path = request.getRequestURI();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(null, HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(), message, path, detalles));
    }

    private FieldErrorDetail toDetail(FieldError error) {
        return new FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Cuerpo de la solicitud inválido o malformado");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Falta el parámetro requerido '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no soportado: " + ex.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, "Recurso no encontrado: " + ex.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
