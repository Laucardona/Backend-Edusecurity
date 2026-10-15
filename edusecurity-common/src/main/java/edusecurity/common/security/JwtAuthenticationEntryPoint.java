package edusecurity.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.common.error.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Devuelve 401 en formato RFC 9457 cuando no hay token válido.
 * Se registra como bean desde {@link SecurityAutoConfiguration}.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        escribir401(objectMapper, request, response, "No autorizado: se requiere un token JWT válido");
    }

    static void escribir401(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                            String detalle) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                Problemas.crear(HttpStatus.UNAUTHORIZED, detalle, request.getRequestURI()));
    }
}
