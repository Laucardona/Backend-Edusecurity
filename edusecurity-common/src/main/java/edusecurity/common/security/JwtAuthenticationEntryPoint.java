package edusecurity.common.security;

import edusecurity.common.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Devuelve 401 con el formato de error estándar cuando no hay token válido.
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
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI();
        String mensaje = (authException != null && authException.getMessage() != null)
                ? authException.getMessage()
                : "No autorizado: se requiere un token JWT válido";
        objectMapper.writeValue(response.getWriter(),
                ApiError.of(401, "Unauthorized", mensaje, path));
    }
}
