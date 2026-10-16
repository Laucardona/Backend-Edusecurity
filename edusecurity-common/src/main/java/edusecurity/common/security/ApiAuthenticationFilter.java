package edusecurity.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica cada petición de dos formas:
 *
 * <ol>
 *   <li><b>Bearer JWT</b>: valida firma, vigencia y tipo "access" del token.</li>
 *   <li><b>X-Service-Key</b>: clave estática para llamadas servicio a servicio
 *       (autenticación interna con rol INTERNAL).</li>
 * </ol>
 */
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_KEY_HEADER = "X-Service-Key";

    private final JwtSupport jwtSupport;
    private final ApiSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public ApiAuthenticationFilter(JwtSupport jwtSupport, ApiSecurityProperties properties,
                                   ObjectMapper objectMapper) {
        this.jwtSupport = jwtSupport;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                TokenInfo info = jwtSupport.parseAccessToken(token);
                authenticate(request, info);
                filterChain.doFilter(request, response);
            } catch (RuntimeException ex) {
                writeUnauthorized(response, request, "Token inválido o expirado");
            }
            return;
        }

        String serviceKey = request.getHeader(INTERNAL_KEY_HEADER);
        String configuredKey = properties.getInternalKey();
        if (serviceKey != null && !serviceKey.isBlank()
                && configuredKey != null && !configuredKey.isBlank()
                && configuredKey.equals(serviceKey)) {
            authenticate(request, new TokenInfo("internal", null, "INTERNAL", "service-key"));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, TokenInfo info) {
        String rol = (info.rol() == null) ? "" : info.rol();
        // ROLE_<rol> para las reglas por rol (p. ej. INTERNAL) y cada permiso como authority
        // para @PreAuthorize("hasAuthority('<permiso>')") (ADR-012).
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
        info.permisos().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(info, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request,
                                   String mensaje) throws IOException {
        JwtAuthenticationEntryPoint.escribir401(objectMapper, request, response, mensaje);
    }
}
