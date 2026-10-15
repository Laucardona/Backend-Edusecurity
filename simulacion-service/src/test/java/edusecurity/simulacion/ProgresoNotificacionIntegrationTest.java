package edusecurity.simulacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.common.security.JwtSupport;
import edusecurity.simulacion.client.ProgresoClient;
import edusecurity.simulacion.client.SimulacionCompletadaPayload;
import edusecurity.simulacion.entity.SesionSimulacion;
import edusecurity.simulacion.repository.SesionSimulacionRepository;
import edusecurity.simulacion.service.NotificadorProgreso;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Envío de resultados a progreso-service (I7.4): después del commit, una vez por sesión,
 * sin afectar la respuesta al estudiante si progreso falla, y con reintento programado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProgresoNotificacionIntegrationTest {

    private static final String PHISHING = "6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01";
    private static final String LABORATORIO = "a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private SesionSimulacionRepository sesionRepository;

    @Autowired
    private NotificadorProgreso notificador;

    @MockitoBean
    private ProgresoClient progresoClient;

    private String token(long usuarioId) {
        return "Bearer " + jwtSupport.generateAccessToken("u" + usuarioId + "@test.com", usuarioId, "ESTUDIANTE");
    }

    private String iniciar(String escenario, long usuarioId) throws Exception {
        String json = mockMvc.perform(post("/api/simulaciones/" + escenario + "/sesiones").header("Authorization", token(usuarioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("uuid").asText();
    }

    private void finalizar(String sesion, long usuarioId) throws Exception {
        mockMvc.perform(post("/api/simulaciones/sesiones/" + sesion + "/finalizar").header("Authorization", token(usuarioId)))
                .andExpect(status().isOk());
    }

    private boolean notificada(String sesion) {
        return sesionRepository.findByUuid(sesion).orElseThrow().getProgresoNotificado();
    }

    private SimulacionCompletadaPayload enviadoPara(String sesion) {
        ArgumentCaptor<SimulacionCompletadaPayload> captor = ArgumentCaptor.forClass(SimulacionCompletadaPayload.class);
        verify(progresoClient, times(1)).registrarSimulacion(argThat(p -> p.sesionUuid().equals(sesion)));
        verify(progresoClient, org.mockito.Mockito.atLeastOnce()).registrarSimulacion(captor.capture());
        return captor.getAllValues().stream().filter(p -> p.sesionUuid().equals(sesion)).findFirst().orElseThrow();
    }

    @Test
    void alFinalizarSeEnviaUnaVezConElResultadoYSeMarcaNotificada() throws Exception {
        String sesion = iniciar(PHISHING, 400);
        finalizar(sesion, 400);
        // Finalizar de nuevo no vuelve a cerrar ni a enviar.
        finalizar(sesion, 400);

        SimulacionCompletadaPayload p = enviadoPara(sesion);
        assertThat(p.usuarioId()).isEqualTo(400L);
        assertThat(p.escenarioUuid()).isEqualTo(PHISHING);
        assertThat(p.tipo()).isEqualTo("PHISHING_DIRIGIDO");
        assertThat(p.estado()).isEqualTo("FINALIZADA");
        assertThat(p.puntaje()).isZero();
        assertThat(p.puntajeMaximo()).isPositive();
        assertThat(p.aprobado()).isFalse();
        assertThat(p.finalizadaEn()).isNotNull();
        assertThat(notificada(sesion)).isTrue();
    }

    @Test
    void elUltimoPasoDeUnLaboratorioEnviaElResultadoAprobado() throws Exception {
        String sesion = iniciar(LABORATORIO, 410);
        String[][] camino = {{"ALERTA", "B"}, {"REGISTRO", "C"}, {"SESIONES", "A"}, {"MFA", "C"}, {"REGLA", "B"}, {"REPORTE", "B"}};
        for (String[] paso : camino) {
            mockMvc.perform(post("/api/simulaciones/sesiones/" + sesion + "/acciones")
                            .header("Authorization", token(410)).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("pasoCodigo", paso[0], "opcionCodigo", paso[1]))))
                    .andExpect(status().isOk());
        }

        SimulacionCompletadaPayload p = enviadoPara(sesion);
        assertThat(p.tipo()).isEqualTo("LABORATORIO");
        assertThat(p.puntaje()).isEqualTo(90);
        assertThat(p.puntajeMaximo()).isEqualTo(90);
        assertThat(p.porcentaje()).isEqualTo(100.0);
        assertThat(p.aprobado()).isTrue();
        assertThat(notificada(sesion)).isTrue();
    }

    @Test
    void unaSesionVencidaTambienSeEnvia() throws Exception {
        String sesion = iniciar(LABORATORIO, 420);
        SesionSimulacion entidad = sesionRepository.findByUuid(sesion).orElseThrow();
        entidad.setVenceEn(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        sesionRepository.saveAndFlush(entidad);

        mockMvc.perform(get("/api/simulaciones/sesiones/" + sesion + "/resultado").header("Authorization", token(420)))
                .andExpect(status().isOk());

        assertThat(enviadoPara(sesion).estado()).isEqualTo("VENCIDA");
        assertThat(notificada(sesion)).isTrue();
    }

    @Test
    void siProgresoFallaElEstudianteRecibe200YElReintentoLoEnviaDespues() throws Exception {
        String sesion = iniciar(PHISHING, 430);
        doThrow(new RuntimeException("progreso-service caído")).when(progresoClient).registrarSimulacion(any());

        finalizar(sesion, 430);
        assertThat(notificada(sesion)).isFalse();

        doNothing().when(progresoClient).registrarSimulacion(any());
        // Otras clases de prueba comparten la base y dejan sesiones pendientes: se procesan lotes hasta vaciar.
        for (int i = 0; i < 20 && !notificada(sesion); i++) {
            notificador.reintentarPendientes();
        }

        assertThat(notificada(sesion)).isTrue();
        verify(progresoClient, times(2)).registrarSimulacion(argThat(p -> p.sesionUuid().equals(sesion)));
    }
}
