package edusecurity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.auth.notification.CorreoService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Historias de M01 agregadas: cierre de sesión (HU3), recuperación de contraseña (HU4),
 * política de contraseñas (HU36), historial de sesiones (HU37) y bloqueo (HU38).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccesoM01IntegrationTest {

    private static final String PASSWORD = "Segura123*";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CorreoService correoService;

    // ------------------------------------------------------------------ HU36

    @Test
    void registroRechazaContraseñaDebilConDetalle() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Débil","correo":"debil@correo.com","password":"abcdefgh"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void indicadorDeFortaleza() throws Exception {
        mockMvc.perform(post("/api/auth/password/fortaleza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("DEBIL"))
                .andExpect(jsonPath("$.cumplePolitica").value(false));

        mockMvc.perform(post("/api/auth/password/fortaleza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"UnaClave-Muy-Larga-2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("FUERTE"))
                .andExpect(jsonPath("$.cumplePolitica").value(true))
                .andExpect(jsonPath("$.faltantes").isEmpty());
    }

    // ------------------------------------------------------------------ HU38

    @Test
    void bloqueaTrasCincoIntentosYAdminPuedeDesbloquear() throws Exception {
        String correo = "bloqueo@correo.com";
        Long id = registrar(correo);

        for (int i = 1; i <= 4; i++) {
            login(correo, "Incorrecta1*").andExpect(status().isUnauthorized());
        }
        login(correo, "Incorrecta1*").andExpect(status().isLocked());

        // Bloqueada: ni con la contraseña correcta entra
        login(correo, PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.type").value("urn:edusecurity:problema:cuenta-bloqueada"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("bloqueada")));

        String admin = token(login("admin@edusecurity.com", "Admin123*"), "accessToken");
        mockMvc.perform(patch("/api/usuarios/" + id + "/estado")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueadoHasta").doesNotExist());

        login(correo, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void loginCorrectoReiniciaElContadorDeFallos() throws Exception {
        String correo = "contador@correo.com";
        registrar(correo);

        for (int i = 0; i < 4; i++) {
            login(correo, "Incorrecta1*").andExpect(status().isUnauthorized());
        }
        login(correo, PASSWORD).andExpect(status().isOk());
        login(correo, "Incorrecta1*").andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- HU3 / HU37

    @Test
    void logoutInvalidaElRefreshTokenDeEsaSesion() throws Exception {
        String correo = "logout@correo.com";
        registrar(correo);
        String respuesta = login(correo, PASSWORD).andReturn().getResponse().getContentAsString();
        String access = extraer(respuesta, "accessToken");
        String refresh = extraer(respuesta, "refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isNoContent());

        refrescar(refresh).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenSoloSePuedeUsarUnaVez() throws Exception {
        String correo = "rotacion@correo.com";
        registrar(correo);
        String refresh = token(login(correo, PASSWORD), "refreshToken");

        String nuevo = token(refrescar(refresh).andExpect(status().isOk()), "refreshToken");
        assertThat(nuevo).isNotEqualTo(refresh);

        refrescar(refresh).andExpect(status().isUnauthorized());
        refrescar(nuevo).andExpect(status().isOk());
    }

    @Test
    void logoutDeTodasLasSesiones() throws Exception {
        String correo = "todas@correo.com";
        registrar(correo);
        String refresh1 = token(login(correo, PASSWORD), "refreshToken");
        String respuesta2 = login(correo, PASSWORD).andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + extraer(respuesta2, "accessToken"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"todas\":true}"))
                .andExpect(status().isNoContent());

        refrescar(refresh1).andExpect(status().isUnauthorized());
        refrescar(extraer(respuesta2, "refreshToken")).andExpect(status().isUnauthorized());
    }

    @Test
    void historialDeSesionesConDispositivo() throws Exception {
        String correo = "historial@correo.com";
        registrar(correo);
        login(correo, PASSWORD);
        String access = token(mockMvc.perform(post("/api/auth/login")
                .header("User-Agent", "Firefox de prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credenciales(correo, PASSWORD))), "accessToken");

        mockMvc.perform(get("/api/auth/sesiones").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[0].dispositivo").value("Firefox de prueba"))
                .andExpect(jsonPath("$.content[0].activa").value(true))
                .andExpect(jsonPath("$.content[0].inicio").isNotEmpty());
    }

    // ------------------------------------------------------------------- HU4

    @Test
    void recuperarYRestablecerContraseña() throws Exception {
        String correo = "olvido@correo.com";
        registrar(correo);
        String refreshViejo = token(login(correo, PASSWORD), "refreshToken");

        mockMvc.perform(post("/api/auth/password/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\"}"))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarRecuperacion(eq(correo), anyString(), enlace.capture());
        String tokenRecuperacion = URLDecoder.decode(
                enlace.getValue().substring(enlace.getValue().indexOf("token=") + 6), StandardCharsets.UTF_8);

        String body = "{\"token\":\"" + tokenRecuperacion + "\",\"passwordNuevo\":\"Nueva45678#\"}";
        mockMvc.perform(post("/api/auth/password/restablecer")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        login(correo, PASSWORD).andExpect(status().isUnauthorized());
        login(correo, "Nueva45678#").andExpect(status().isOk());
        // Cambiar la contraseña cierra las sesiones abiertas
        refrescar(refreshViejo).andExpect(status().isUnauthorized());
        // El token es de un solo uso
        mockMvc.perform(post("/api/auth/password/restablecer")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recuperarConCorreoInexistenteRespondeIgualYNoEnvia() throws Exception {
        mockMvc.perform(post("/api/auth/password/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@correo.com\"}"))
                .andExpect(status().isAccepted());

        verify(correoService, never()).enviarRecuperacion(eq("nadie@correo.com"), any(), any());
    }

    @Test
    void restablecerConTokenInventadoFalla() throws Exception {
        mockMvc.perform(post("/api/auth/password/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"inventado\",\"passwordNuevo\":\"Nueva45678#\"}"))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------------- helpers

    private Long registrar(String correo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Prueba\",\"correo\":\"" + correo + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }

    private ResultActions login(String correo, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credenciales(correo, password)));
    }

    private ResultActions refrescar(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private String credenciales(String correo, String password) {
        return "{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
    }

    private String token(ResultActions resultado, String campo) throws Exception {
        return extraer(resultado.andReturn().getResponse().getContentAsString(), campo);
    }

    private String extraer(String json, String campo) throws Exception {
        JsonNode nodo = objectMapper.readTree(json);
        return nodo.get(campo).asText();
    }
}
