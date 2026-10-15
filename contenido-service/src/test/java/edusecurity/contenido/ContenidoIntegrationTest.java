package edusecurity.contenido;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.common.security.JwtSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CRUD de módulos y lecciones sobre H2. El token se genera directamente con
 * JwtSupport (misma clave que en producción) para probar el control de acceso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContenidoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    private static String adminToken;
    private static String estudianteToken;
    private static Long moduloId;
    private static Long leccionId;

    private String token(String rol) {
        return jwtSupport.generateAccessToken("test@" + rol.toLowerCase() + ".com", 1L, rol);
    }

    @Test
    @Order(1)
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/modulos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void usuarioAutenticadoPuedeLeer() throws Exception {
        estudianteToken = token("ESTUDIANTE");
        adminToken = token("ADMIN");

        mockMvc.perform(get("/api/modulos")
                        .header("Authorization", "Bearer " + estudianteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(3)
    void estudianteNoPuedeCrearModulo() throws Exception {
        String body = "{\"titulo\":\"No permitido\"}";

        mockMvc.perform(post("/api/modulos")
                        .header("Authorization", "Bearer " + estudianteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void adminCreaModuloConValidaciones() throws Exception {
        // Sin título -> 400 con detalle de campos
        mockMvc.perform(post("/api/modulos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        String body = """
                {"titulo":"Fundamentos de Ciberseguridad",
                 "descripcion":"Módulo inicial del curso",
                 "orden":1,
                 "activo":true}
                """;

        String respuesta = mockMvc.perform(post("/api/modulos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Fundamentos de Ciberseguridad"))
                .andExpect(jsonPath("$.totalLecciones").value(0))
                .andReturn().getResponse().getContentAsString();

        moduloId = leerId(respuesta);
    }

    @Test
    @Order(5)
    void validaCamposObligatorios() throws Exception {
        mockMvc.perform(post("/api/modulos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"sin titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("titulo"));
    }

    @Test
    @Order(6)
    void adminCreaLeccionAsociadaAlModulo() throws Exception {
        String body = """
                {"titulo":"¿Qué es la seguridad informática?",
                 "descripcion":"Introducción",
                 "contenido":"La seguridad informática protege los sistemas...",
                 "orden":1,
                 "moduloId":%d}
                """.formatted(moduloId);

        String respuesta = mockMvc.perform(post("/api/lecciones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modulo.id").value(moduloId))
                .andReturn().getResponse().getContentAsString();

        leccionId = leerId(respuesta);

        mockMvc.perform(get("/api/modulos/" + moduloId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLecciones").value(1));

        mockMvc.perform(get("/api/lecciones/modulo/" + moduloId)
                        .header("Authorization", "Bearer " + estudianteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @Order(7)
    void rechazaLeccionConModuloInexistente() throws Exception {
        String body = """
                {"titulo":"Huerfana","moduloId":99999}
                """;

        mockMvc.perform(post("/api/lecciones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void actualizaModulo() throws Exception {
        String body = """
                {"titulo":"Fundamentos de Ciberseguridad v2","orden":2}
                """;

        mockMvc.perform(put("/api/modulos/" + moduloId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Fundamentos de Ciberseguridad v2"));
    }

    @Test
    @Order(9)
    void noEliminaModuloConLeccionesSinForce() throws Exception {
        mockMvc.perform(delete("/api/modulos/" + moduloId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/lecciones/" + leccionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/modulos/" + moduloId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    private Long leerId(String json) throws Exception {
        JsonNode nodo = objectMapper.readTree(json);
        return nodo.get("id").asLong();
    }
}
