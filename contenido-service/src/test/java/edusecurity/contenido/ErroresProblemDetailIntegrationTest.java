package edusecurity.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edusecurity.common.security.JwtSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Errores RFC 9457 e id de correlación (tarea 0.10), vistos desde un servicio real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErroresProblemDetailIntegrationTest {

    private static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtSupport jwtSupport;

    private String token() {
        return "Bearer " + jwtSupport.generateAccessToken("e@test.com", 7L, "ESTUDIANTE");
    }

    @Test
    void unRecursoInexistenteDevuelveProblemDetailConElIdDeCorrelacionRecibido() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/modulos/987654").header("Authorization", token()).header("X-Correlation-Id", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Correlation-Id", id))
                .andExpect(jsonPath("$.type").value("urn:edusecurity:problema:no-encontrado"))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/modulos/987654"))
                .andExpect(jsonPath("$.correlationId").value(id))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unIdDeCorrelacionInseguroSeReemplazaPorUnoNuevo() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/modulos/987654").header("Authorization", token())
                        .header("X-Correlation-Id", "<script>alert(1)</script>"))
                .andExpect(status().isNotFound())
                .andReturn();

        String id = r.getResponse().getHeader("X-Correlation-Id");
        assertThat(id).matches(UUID_REGEX);
        assertThat(r.getResponse().getContentAsString()).contains(id).doesNotContain("script");

        String largo = "a".repeat(65);
        String otro = mockMvc.perform(get("/api/modulos").header("Authorization", token()).header("X-Correlation-Id", largo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Correlation-Id");
        assertThat(otro).matches(UUID_REGEX);
    }

    @Test
    void sinTokenEl401TambienEsProblemDetailConCorrelacion() throws Exception {
        mockMvc.perform(get("/api/modulos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:edusecurity:problema:no-autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(header().exists("X-Correlation-Id"));
    }
}
