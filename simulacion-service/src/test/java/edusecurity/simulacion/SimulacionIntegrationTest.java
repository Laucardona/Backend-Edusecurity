package edusecurity.simulacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Recorrido completo del laboratorio sobre H2 con el escenario sembrado en V2:
 * catálogo, inicio, respuestas, control de acceso, tiempo límite y retroalimentación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimulacionIntegrationTest {

    private static final String ESCENARIO = "6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private SesionSimulacionRepository sesionRepository;

    private String token(long usuarioId, String rol) {
        return "Bearer " + jwtSupport.generateAccessToken("u" + usuarioId + "@test.com", usuarioId, rol);
    }

    private JsonNode json(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode iniciar(long usuarioId) throws Exception {
        return json(mockMvc.perform(post("/api/simulaciones/" + ESCENARIO + "/sesiones")
                        .header("Authorization", token(usuarioId, "ESTUDIANTE")))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long mensajeId(JsonNode sesion, int orden) {
        for (JsonNode m : sesion.get("bandeja")) {
            if (m.get("orden").asInt() == orden) {
                return m.get("id").asLong();
            }
        }
        throw new IllegalStateException("No hay mensaje con orden " + orden);
    }

    private String respuesta(String clasificacion, String accion, String... indicadores) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "clasificacion", clasificacion, "accion", accion, "indicadores", indicadores));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/simulaciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void elCatalogoMuestraElEscenarioSembradoYLasSenales() throws Exception {
        mockMvc.perform(get("/api/simulaciones").header("Authorization", token(10, "ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.uuid == '" + ESCENARIO + "')].totalMensajes").value(7));

        mockMvc.perform(get("/api/simulaciones/indicadores").header("Authorization", token(10, "ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void laBandejaNoRevelaCualesSonPhishing() throws Exception {
        JsonNode sesion = iniciar(20);

        assertThat(sesion.get("estado").asText()).isEqualTo("EN_CURSO");
        assertThat(sesion.get("segundosRestantes").asLong()).isBetween(590L, 600L);
        assertThat(sesion.get("bandeja")).hasSize(7);
        String bandeja = sesion.get("bandeja").toString();
        assertThat(bandeja).doesNotContain("esPhishing", "accionCorrecta", "explicacion", "indicadores\":[{");
    }

    @Test
    void iniciarDosVecesDevuelveLaMismaSesion() throws Exception {
        JsonNode primera = iniciar(30);
        JsonNode segunda = iniciar(30);
        assertThat(segunda.get("uuid").asText()).isEqualTo(primera.get("uuid").asText());
    }

    @Test
    void recorridoCompletoConRetroalimentacion() throws Exception {
        JsonNode sesion = iniciar(40);
        String uuid = sesion.get("uuid").asText();
        String auth = token(40, "ESTUDIANTE");

        // Mensaje 1 (whaling): detectado con 3 de sus 6 señales
        mockMvc.perform(put("/api/simulaciones/sesiones/" + uuid + "/respuestas/" + mensajeId(sesion, 1))
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(respuesta("PHISHING", "REPORTAR", "DOMINIO_SIMILAR", "URGENCIA", "SECRETISMO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.length()").value(3));

        // Mensaje 2 (legítimo): falso positivo
        mockMvc.perform(put("/api/simulaciones/sesiones/" + uuid + "/respuestas/" + mensajeId(sesion, 2))
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(respuesta("PHISHING", "REPORTAR")))
                .andExpect(status().isOk());

        // Mensaje 3 (factura falsa): cae y abre el enlace
        mockMvc.perform(put("/api/simulaciones/sesiones/" + uuid + "/respuestas/" + mensajeId(sesion, 3))
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(respuesta("LEGITIMO", "ABRIR_ENLACE")))
                .andExpect(status().isOk());

        // El resultado no está disponible mientras la sesión sigue abierta
        mockMvc.perform(get("/api/simulaciones/sesiones/" + uuid + "/resultado").header("Authorization", auth))
                .andExpect(status().isConflict());

        JsonNode resultado = json(mockMvc.perform(post("/api/simulaciones/sesiones/" + uuid + "/finalizar")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultado.get("estado").asText()).isEqualTo("FINALIZADA");
        assertThat(resultado.get("mensajesRespondidos").asInt()).isEqualTo(3);
        assertThat(resultado.get("totalPhishing").asInt()).isEqualTo(4);
        assertThat(resultado.get("phishingDetectados").asInt()).isEqualTo(1);
        assertThat(resultado.get("falsosPositivos").asInt()).isEqualTo(1);
        assertThat(resultado.get("accionesPeligrosas").asInt()).isEqualTo(1);
        assertThat(resultado.get("aprobado").asBoolean()).isFalse();

        JsonNode m1 = resultado.get("mensajes").get(0);
        assertThat(m1.get("esPhishing").asBoolean()).isTrue();
        assertThat(m1.get("senalesEncontradas")).hasSize(3);
        assertThat(m1.get("senalesOmitidas")).hasSize(3);
        assertThat(m1.get("senalesOmitidas").toString()).contains("te paso los datos nuevos por WhatsApp");
        assertThat(m1.get("puntaje").asInt()).isEqualTo(10 + 3 + 6);

        // Finalizar de nuevo devuelve el mismo resultado y ya no se aceptan respuestas
        mockMvc.perform(post("/api/simulaciones/sesiones/" + uuid + "/finalizar").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntaje").value(resultado.get("puntaje").asInt()));
        mockMvc.perform(put("/api/simulaciones/sesiones/" + uuid + "/respuestas/" + mensajeId(sesion, 4))
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(respuesta("LEGITIMO", "CONSERVAR")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/simulaciones/sesiones/mias").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].uuid").value(uuid));
    }

    @Test
    void otroEstudianteNoPuedeVerNiResponderUnaSesionAjena() throws Exception {
        JsonNode sesion = iniciar(50);
        String uuid = sesion.get("uuid").asText();
        String intruso = token(51, "ESTUDIANTE");

        mockMvc.perform(get("/api/simulaciones/sesiones/" + uuid).header("Authorization", intruso))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/simulaciones/sesiones/" + uuid + "/respuestas/" + mensajeId(sesion, 1))
                        .header("Authorization", intruso).contentType(MediaType.APPLICATION_JSON)
                        .content(respuesta("PHISHING", "REPORTAR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/simulaciones/sesiones/" + uuid + "/finalizar").header("Authorization", intruso))
                .andExpect(status().isNotFound());
    }

    @Test
    void cuandoSeAcabaElTiempoLaSesionQuedaVencidaYNoAceptaRespuestas() throws Exception {
        JsonNode sesion = iniciar(60);
        String uuid = sesion.get("uuid").asText();
        String auth = token(60, "ESTUDIANTE");

        SesionSimulacion entidad = sesionRepository.findByUuid(uuid).orElseThrow();
        entidad.setVenceEn(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        sesionRepository.saveAndFlush(entidad);

        mockMvc.perform(put("/api/simulaciones/sesiones/" + uuid + "/respuestas/" + mensajeId(sesion, 1))
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(respuesta("PHISHING", "REPORTAR")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/simulaciones/sesiones/" + uuid + "/resultado").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VENCIDA"))
                .andExpect(jsonPath("$.mensajesRespondidos").value(0));
    }

    @Test
    void soloAdminCreaEscenariosYLosBorradoresNoSonVisibles() throws Exception {
        String cuerpo = """
                {
                  "tipo": "PHISHING_CORREO",
                  "titulo": "Premio falso",
                  "contexto": "Eres estudiante y revisas tu correo personal.",
                  "nivel": "BASICO",
                  "duracionSegundos": 300,
                  "mensajes": [{
                    "canal": "CORREO",
                    "remitenteNombre": "Sorteos",
                    "remitenteDireccion": "premios@sorteo-ganador.example",
                    "asunto": "¡Ganaste!",
                    "cuerpo": "Reclama tu premio hoy.",
                    "esPhishing": true,
                    "accionCorrecta": "ELIMINAR",
                    "explicacion": "Nadie regala premios por correo.",
                    "indicadores": [{"tipo": "URGENCIA", "descripcion": "Presiona para actuar hoy", "fragmento": "hoy"}]
                  }]
                }
                """;

        mockMvc.perform(post("/api/simulaciones").header("Authorization", token(1, "ESTUDIANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());

        JsonNode creado = json(mockMvc.perform(post("/api/simulaciones").header("Authorization", token(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicado").value(false))
                .andReturn());
        String uuid = creado.get("uuid").asText();

        mockMvc.perform(get("/api/simulaciones/" + uuid).header("Authorization", token(70, "ESTUDIANTE")))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/simulaciones/" + uuid + "/publicacion").header("Authorization", token(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\": true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/simulaciones/" + uuid).header("Authorization", token(70, "ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMensajes").value(1));
    }

    @Test
    void rechazaUnPhishingSinSenalesDeAlerta() throws Exception {
        String cuerpo = """
                {
                  "tipo": "PHISHING_CORREO", "titulo": "Incompleto", "contexto": "Contexto", "nivel": "BASICO",
                  "duracionSegundos": 300,
                  "mensajes": [{
                    "canal": "CORREO", "remitenteNombre": "X", "remitenteDireccion": "x@x.example",
                    "cuerpo": "Hola", "esPhishing": true, "accionCorrecta": "REPORTAR", "explicacion": "..."
                  }]
                }
                """;
        mockMvc.perform(post("/api/simulaciones").header("Authorization", token(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
