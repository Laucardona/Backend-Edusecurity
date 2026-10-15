package edusecurity.progreso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.common.security.JwtSupport;
import edusecurity.common.web.PageResponse;
import edusecurity.progreso.client.AuthClient;
import edusecurity.progreso.client.ContenidoClient;
import edusecurity.progreso.client.UsuarioFeign;
import edusecurity.progreso.repository.SimulacionCompletadaRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Resultados de simulaciones (I7.4): endpoint interno idempotente, historial del estudiante
 * y conteos en el resumen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimulacionesCompletadasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private SimulacionCompletadaRepository repository;

    @Value("${app.security.internal-key}")
    private String claveInterna;

    @MockitoBean
    private AuthClient authClient;

    @MockitoBean
    private ContenidoClient contenidoClient;

    @BeforeEach
    void setUp() {
        when(authClient.obtenerUsuario(anyLong())).thenReturn(new UsuarioFeign(1L, "Estudiante", "e@test.com", true));
        when(contenidoClient.listarLecciones(anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 5000, 0, 0, true, true));
    }

    private String token(long usuarioId, String rol) {
        return "Bearer " + jwtSupport.generateAccessToken("u" + usuarioId + "@test.com", usuarioId, rol);
    }

    private String cuerpo(long usuarioId, String sesionUuid, int puntaje, boolean aprobado, LocalDateTime finalizadaEn)
            throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("usuarioId", usuarioId);
        m.put("sesionUuid", sesionUuid);
        m.put("escenarioUuid", "a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22");
        m.put("tipo", "LABORATORIO");
        m.put("titulo", "Respuesta a incidente");
        m.put("puntaje", puntaje);
        m.put("puntajeMaximo", 90);
        m.put("porcentaje", Math.round(puntaje * 1000.0 / 90) / 10.0);
        m.put("aprobado", aprobado);
        m.put("estado", "FINALIZADA");
        m.put("finalizadaEn", finalizadaEn.toString());
        return objectMapper.writeValueAsString(m);
    }

    private ResultActions registrar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/internal/progreso/simulaciones")
                .header("X-Service-Key", claveInterna)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void conLaClaveInternaCreaYAlRepetirNoDuplica() throws Exception {
        String sesion = UUID.randomUUID().toString();
        String body = cuerpo(300, sesion, 90, true, LocalDateTime.of(2026, 10, 15, 10, 0));

        Long id = objectMapper.readTree(registrar(body)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.sesionUuid").value(sesion))
                        .andExpect(jsonPath("$.aprobado").value(true))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        registrar(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        assertThat(repository.findAll().stream().filter(s -> s.getSesionUuid().equals(sesion))).hasSize(1);
    }

    @Test
    void unEstudianteRecibe403YSinAutenticacion401() throws Exception {
        String body = cuerpo(310, UUID.randomUUID().toString(), 50, false, LocalDateTime.now());

        mockMvc.perform(post("/internal/progreso/simulaciones").header("Authorization", token(310, "ESTUDIANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/progreso/simulaciones")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/progreso/simulaciones").header("X-Service-Key", "clave-equivocada")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaUnCuerpoInvalido() throws Exception {
        registrar("{\"usuarioId\": 1, \"estado\": \"EN_CURSO\"}").andExpect(status().isBadRequest());
    }

    @Test
    void misSimulacionesMuestraSoloLasPropiasDeLaMasRecienteALaMasAntigua() throws Exception {
        String antigua = UUID.randomUUID().toString();
        String reciente = UUID.randomUUID().toString();
        registrar(cuerpo(320, antigua, 40, false, LocalDateTime.of(2026, 10, 1, 9, 0))).andExpect(status().isCreated());
        registrar(cuerpo(320, reciente, 80, true, LocalDateTime.of(2026, 10, 14, 9, 0))).andExpect(status().isCreated());
        registrar(cuerpo(321, UUID.randomUUID().toString(), 90, true, LocalDateTime.of(2026, 10, 15, 9, 0)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/progreso/simulaciones/mias").header("Authorization", token(320, "ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].sesionUuid").value(reciente))
                .andExpect(jsonPath("$.content[1].sesionUuid").value(antigua))
                .andExpect(jsonPath("$.content[?(@.usuarioId != 320)]").isEmpty());
    }

    @Test
    void elResumenCuentaLasSimulacionesCompletadasYAprobadas() throws Exception {
        registrar(cuerpo(330, UUID.randomUUID().toString(), 90, true, LocalDateTime.now())).andExpect(status().isCreated());
        registrar(cuerpo(330, UUID.randomUUID().toString(), 30, false, LocalDateTime.now())).andExpect(status().isCreated());
        registrar(cuerpo(330, UUID.randomUUID().toString(), 70, true, LocalDateTime.now())).andExpect(status().isCreated());

        mockMvc.perform(get("/api/progreso/resumen/mio").header("Authorization", token(330, "ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulacionesCompletadas").value(3))
                .andExpect(jsonPath("$.simulacionesAprobadas").value(2));

        mockMvc.perform(get("/api/progreso/resumen/usuario/330").header("Authorization", token(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulacionesCompletadas").value(3))
                .andExpect(jsonPath("$.simulacionesAprobadas").value(2));
    }
}
