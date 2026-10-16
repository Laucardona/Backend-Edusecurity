package edusecurity.simulacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.common.security.JwtSupport;
import edusecurity.simulacion.entity.SesionSimulacion;
import edusecurity.simulacion.repository.SesionSimulacionRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Laboratorio guiado (HU44) sobre H2 con el escenario sembrado en V4: máquina de estados,
 * respuestas ocultas, camino óptimo, recuperación, control de acceso, tiempo límite y tipos cruzados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LaboratorioGuiadoIntegrationTest {

    private static final String LABORATORIO = "a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22";
    private static final String PHISHING = "6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01";

    /** Camino óptimo del escenario sembrado: 6 pasos de 15 puntos. */
    private static final String[][] CAMINO_OPTIMO = {
            {"ALERTA", "B"}, {"REGISTRO", "C"}, {"SESIONES", "A"}, {"MFA", "C"}, {"REGLA", "B"}, {"REPORTE", "B"}};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private SesionSimulacionRepository sesionRepository;

    private String token(long usuarioId) {
        return "Bearer " + jwtSupport.generateAccessToken("u" + usuarioId + "@test.com", usuarioId, "ESTUDIANTE");
    }

    private JsonNode json(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode iniciar(String escenario, long usuarioId) throws Exception {
        return json(mockMvc.perform(post("/api/simulaciones/" + escenario + "/sesiones").header("Authorization", token(usuarioId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions accion(String sesion, long usuarioId, String paso, String opcion) throws Exception {
        return mockMvc.perform(post("/api/simulaciones/sesiones/" + sesion + "/acciones")
                .header("Authorization", token(usuarioId)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pasoCodigo", paso, "opcionCodigo", opcion))));
    }

    private ResultActions paso(String sesion, long usuarioId) throws Exception {
        return mockMvc.perform(get("/api/simulaciones/sesiones/" + sesion + "/paso").header("Authorization", token(usuarioId)));
    }

    @Test
    void alIniciarEmpiezaEnElPrimerPasoSinRevelarRespuestas() throws Exception {
        JsonNode sesion = iniciar(LABORATORIO, 200);
        String uuid = sesion.get("uuid").asText();

        assertThat(sesion.get("escenario").get("tipo").asText()).isEqualTo("LABORATORIO");
        assertThat(sesion.get("escenario").get("totalPasos").asInt()).isEqualTo(7);
        assertThat(sesion.get("segundosRestantes").asLong()).isBetween(890L, 900L);
        assertThat(sesion.get("bandeja")).isEmpty();
        assertThat(sesion.get("pasoActual").get("codigo").asText()).isEqualTo("ALERTA");

        JsonNode actual = json(paso(uuid, 200).andExpect(status().isOk()).andReturn());
        assertThat(actual.get("codigo").asText()).isEqualTo("ALERTA");
        assertThat(actual.get("recursoSimulado").asText()).contains("203.0.113.47");
        assertThat(actual.get("opciones")).hasSize(3);
        assertThat(actual.toString()).doesNotContain("puntos", "esCorrecta", "retroalimentacion", "siguiente");
        assertThat(sesion.get("pasoActual").toString()).doesNotContain("puntos", "esCorrecta", "retroalimentacion");

        // Iniciar otra vez devuelve la misma sesión, en el mismo paso.
        assertThat(iniciar(LABORATORIO, 200).get("uuid").asText()).isEqualTo(uuid);
    }

    @Test
    void elCaminoOptimoDa100PorCientoYCierraLaSesion() throws Exception {
        String uuid = iniciar(LABORATORIO, 210).get("uuid").asText();

        for (int i = 0; i < CAMINO_OPTIMO.length; i++) {
            boolean ultimo = i == CAMINO_OPTIMO.length - 1;
            ResultActions r = accion(uuid, 210, CAMINO_OPTIMO[i][0], CAMINO_OPTIMO[i][1])
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correcta").value(true))
                    .andExpect(jsonPath("$.puntos").value(15))
                    .andExpect(jsonPath("$.puntajeAcumulado").value(15 * (i + 1)))
                    .andExpect(jsonPath("$.finalizada").value(ultimo));
            if (!ultimo) {
                r.andExpect(jsonPath("$.siguientePaso.codigo").value(CAMINO_OPTIMO[i + 1][0]));
            }
        }

        JsonNode resultado = json(mockMvc.perform(get("/api/simulaciones/sesiones/" + uuid + "/resultado")
                        .header("Authorization", token(210)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(resultado.get("tipo").asText()).isEqualTo("LABORATORIO");
        assertThat(resultado.get("estado").asText()).isEqualTo("FINALIZADA");
        assertThat(resultado.get("puntaje").asInt()).isEqualTo(90);
        assertThat(resultado.get("puntajeMaximo").asInt()).isEqualTo(90);
        assertThat(resultado.get("porcentaje").asInt()).isEqualTo(100);
        assertThat(resultado.get("aprobado").asBoolean()).isTrue();
        JsonNode recorrido = resultado.get("recorrido");
        assertThat(recorrido).hasSize(6);
        for (JsonNode p : recorrido) {
            assertThat(p.get("correcta").asBoolean()).isTrue();
            assertThat(p.get("opcionElegida").asText()).isEqualTo(p.get("mejorOpcion").asText());
            assertThat(p.get("retroalimentacion").asText()).isNotBlank();
        }

        // La sesión terminó: no hay paso actual ni se aceptan más acciones.
        paso(uuid, 210).andExpect(status().isConflict());
        accion(uuid, 210, "REPORTE", "A").andExpect(status().isConflict());
        mockMvc.perform(get("/api/simulaciones/sesiones/" + uuid).header("Authorization", token(210)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.pasosRespondidos").value(6))
                .andExpect(jsonPath("$.pasoActual").doesNotExist());
    }

    @Test
    void unaMalaDecisionLlevaAlPasoDeRecuperacionYQuedaEnElRecorrido() throws Exception {
        String uuid = iniciar(LABORATORIO, 220).get("uuid").asText();

        accion(uuid, 220, "ALERTA", "A")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correcta").value(false))
                .andExpect(jsonPath("$.puntos").value(0))
                .andExpect(jsonPath("$.retroalimentacion").value(org.hamcrest.Matchers.containsString("dominio oficial")))
                .andExpect(jsonPath("$.siguientePaso.codigo").value("CREDENCIALES"));
        accion(uuid, 220, "CREDENCIALES", "A")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siguientePaso.codigo").value("SESIONES"));

        JsonNode resultado = json(mockMvc.perform(post("/api/simulaciones/sesiones/" + uuid + "/finalizar")
                        .header("Authorization", token(220)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(resultado.get("puntaje").asInt()).isEqualTo(8);
        assertThat(resultado.get("puntajeMaximo").asInt()).isEqualTo(90);
        assertThat(resultado.get("aprobado").asBoolean()).isFalse();
        JsonNode primero = resultado.get("recorrido").get(0);
        assertThat(primero.get("pasoCodigo").asText()).isEqualTo("ALERTA");
        assertThat(primero.get("opcionElegida").asText()).isEqualTo("A");
        assertThat(primero.get("mejorOpcion").asText()).isEqualTo("B");
        assertThat(primero.get("retroalimentacionMejorOpcion").asText()).startsWith("Correcto");
    }

    @Test
    void responderUnPasoQueNoEsElActualDa409() throws Exception {
        String uuid = iniciar(LABORATORIO, 230).get("uuid").asText();

        accion(uuid, 230, "REGISTRO", "C").andExpect(status().isConflict());

        // Tampoco se puede responder dos veces el mismo paso: tras avanzar, ALERTA ya no es el actual.
        accion(uuid, 230, "ALERTA", "B").andExpect(status().isOk());
        accion(uuid, 230, "ALERTA", "A").andExpect(status().isConflict());
    }

    @Test
    void unaOpcionQueNoPerteneceAlPasoDa400() throws Exception {
        String uuid = iniciar(LABORATORIO, 240).get("uuid").asText();

        accion(uuid, 240, "ALERTA", "Z").andExpect(status().isBadRequest());
        // El paso actual no cambió.
        paso(uuid, 240).andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("ALERTA"));
    }

    @Test
    void otroEstudianteNoPuedeVerNiResponderElLaboratorioAjeno() throws Exception {
        String uuid = iniciar(LABORATORIO, 250).get("uuid").asText();

        paso(uuid, 251).andExpect(status().isNotFound());
        accion(uuid, 251, "ALERTA", "B").andExpect(status().isNotFound());
    }

    @Test
    void cuandoSeAcabaElTiempoLaSesionQuedaVencidaYNoAceptaAcciones() throws Exception {
        String uuid = iniciar(LABORATORIO, 260).get("uuid").asText();
        accion(uuid, 260, "ALERTA", "B").andExpect(status().isOk());

        SesionSimulacion entidad = sesionRepository.findByUuid(uuid).orElseThrow();
        entidad.setVenceEn(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        sesionRepository.saveAndFlush(entidad);

        accion(uuid, 260, "REGISTRO", "C").andExpect(status().isConflict());
        paso(uuid, 260).andExpect(status().isConflict());

        mockMvc.perform(get("/api/simulaciones/sesiones/" + uuid + "/resultado").header("Authorization", token(260)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VENCIDA"))
                .andExpect(jsonPath("$.puntaje").value(15))
                .andExpect(jsonPath("$.recorrido.length()").value(1));
    }

    @Test
    void losEndpointsDeCadaTipoRechazanAlOtroCon400() throws Exception {
        String laboratorio = iniciar(LABORATORIO, 270).get("uuid").asText();
        mockMvc.perform(put("/api/simulaciones/sesiones/" + laboratorio + "/respuestas/1")
                        .header("Authorization", token(270)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clasificacion\": \"PHISHING\", \"accion\": \"REPORTAR\"}"))
                .andExpect(status().isBadRequest());

        String phishing = iniciar(PHISHING, 270).get("uuid").asText();
        paso(phishing, 270).andExpect(status().isBadRequest());
        accion(phishing, 270, "ALERTA", "B").andExpect(status().isBadRequest());
    }

    @Test
    void elAdminTodaviaNoPuedeCrearLaboratoriosPorApi() throws Exception {
        String cuerpo = """
                {
                  "tipo": "LABORATORIO", "titulo": "Lab", "contexto": "Contexto", "nivel": "BASICO",
                  "duracionSegundos": 300,
                  "mensajes": [{
                    "canal": "CORREO", "remitenteNombre": "X", "remitenteDireccion": "x@x.example",
                    "cuerpo": "Hola", "esPhishing": false, "accionCorrecta": "CONSERVAR", "explicacion": "..."
                  }]
                }
                """;
        mockMvc.perform(post("/api/simulaciones")
                        .header("Authorization", "Bearer " + jwtSupport.generateAccessToken("admin@test.com", 1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
