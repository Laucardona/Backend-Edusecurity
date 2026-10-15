package edusecurity.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void un500NuncaExponeElMensajeDeLaExcepcion() {
        MDC.put(CorrelationId.MDC_KEY, "abc-123");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/x");

        ResponseEntity<ProblemDetail> r = handler.unexpected(
                new IllegalStateException("jdbc:postgresql://db/secret password=hunter2"), request);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail p = r.getBody();
        assertThat(p.getType().toString()).isEqualTo("urn:edusecurity:problema:error-interno");
        assertThat(p.getDetail()).doesNotContain("jdbc", "hunter2");
        assertThat(p.getInstance().toString()).isEqualTo("/api/x");
        assertThat(p.getProperties()).containsEntry("correlationId", "abc-123").containsKey("timestamp");
    }

    @Test
    void elIdDeCorrelacionAceptaUuidYTokensSegurosYRechazaElResto() {
        String uuid = UUID.randomUUID().toString();
        assertThat(CorrelationId.validarOGenerar(uuid)).isEqualTo(uuid);
        assertThat(CorrelationId.validarOGenerar("pedido-42_a.b")).isEqualTo("pedido-42_a.b");
        assertThat(CorrelationId.validarOGenerar("a".repeat(64))).isEqualTo("a".repeat(64));

        for (String malo : new String[] {null, "", "a".repeat(65), "con espacio", "x\r\nSet-Cookie: y", "<b>"}) {
            String generado = CorrelationId.validarOGenerar(malo);
            assertThat(generado).isNotEqualTo(malo);
            assertThat(UUID.fromString(generado)).isNotNull();
        }
    }
}
