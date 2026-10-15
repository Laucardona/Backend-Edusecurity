package edusecurity.auth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Flujo completo de autenticación sobre H2:
 * registro -> login -> perfil -> control de acceso por roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String estudianteToken;
    private static String adminToken;

    @Test
    @Order(1)
    void adminSembradoPuedeIniciarSesion() throws Exception {
        String body = """
                {"correo":"admin@edusecurity.com","password":"Admin123*"}
                """;

        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol.nombre").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        adminToken = extraer(respuesta, "accessToken");
    }

    @Test
    @Order(2)
    void registraEstudianteYNoExponeContraseña() throws Exception {
        String body = """
                {"nombre":"Estudiante Demo","correo":"estudiante@correo.com","password":"Secreta123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("estudiante@correo.com"))
                .andExpect(jsonPath("$.rol.nombre").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(3)
    void rechazaCorreoDuplicado() throws Exception {
        String body = """
                {"nombre":"Otro","correo":"estudiante@correo.com","password":"Secreta123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    void loginConContraseñaIncorrectaDevuelve401() throws Exception {
        String body = """
                {"correo":"estudiante@correo.com","password":"incorrecta"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void estudianteIniciaSesionYObtieneSuPerfil() throws Exception {
        String body = """
                {"correo":"estudiante@correo.com","password":"Secreta123"}
                """;

        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        estudianteToken = extraer(respuesta, "accessToken");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + estudianteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("estudiante@correo.com"));
    }

    @Test
    @Order(6)
    void refreshTokenGeneraNuevoAccessToken() throws Exception {
        String body = """
                {"correo":"estudiante@correo.com","password":"Secreta123"}
                """;

        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refresh = extraer(respuesta, "refreshToken");

        String refreshBody = "{\"refreshToken\":\"" + refresh + "\"}";
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @Order(7)
    void estudianteNoPuedeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + estudianteToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void adminListaUsuariosPaginados() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(not(0))))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(9)
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    void adminActualizaSuPerfil() throws Exception {
        String body = """
                {"nombre":"Administrador General"}
                """;

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Administrador General"));
    }

    private String extraer(String json, String campo) throws Exception {
        JsonNode nodo = objectMapper.readTree(json);
        return nodo.get(campo).asText();
    }
}
