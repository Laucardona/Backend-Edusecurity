package edusecurity.progreso;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edusecurity.common.security.JwtSupport;
import edusecurity.progreso.client.AuthClient;
import edusecurity.progreso.client.ContenidoClient;
import edusecurity.progreso.client.MetricasUsuariosFeign;
import edusecurity.progreso.dto.SimulacionCompletadaRequest;
import edusecurity.progreso.entity.Progreso;
import edusecurity.progreso.repository.ProgresoRepository;
import edusecurity.progreso.service.SimulacionCompletadaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Panel administrativo (HU33). Las simulaciones se siembran en 2002, un periodo que ninguna otra
 * prueba usa; el progreso de lecciones solo lo siembra esta clase.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PanelAdministrativoIntegrationTest {

    private static final String URL = "/api/analitica/panel";
    private static final String LAB = "a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private ProgresoRepository progresoRepository;

    @Autowired
    private SimulacionCompletadaService simulacionService;

    @MockitoBean
    private AuthClient authClient;

    @MockitoBean
    private ContenidoClient contenidoClient;

    private String token(String rol) {
        return "Bearer " + jwtSupport.generateAccessToken("x@test.com", 1L, rol);
    }

    private ResultActions panel(String desde, String hasta) throws Exception {
        var request = get(URL).header("Authorization", token("ADMIN"));
        if (desde != null) {
            request.param("desde", desde);
        }
        if (hasta != null) {
            request.param("hasta", hasta);
        }
        return mockMvc.perform(request);
    }

    private void simulacion(String escenario, String titulo, String tipo, double porcentaje, String finalizadaEn) {
        simulacionService.registrar(new SimulacionCompletadaRequest(500L, UUID.randomUUID().toString(), escenario,
                tipo, titulo, (int) porcentaje, 100, porcentaje, porcentaje >= 70, "FINALIZADA",
                LocalDateTime.parse(finalizadaEn)));
    }

    private void sembrarSimulacionesDeMayo2002() {
        simulacion(LAB, "Laboratorio", "LABORATORIO", 100, "2002-05-01T00:00:00");
        simulacion(LAB, "Laboratorio", "LABORATORIO", 50, "2002-05-15T10:00:00");
        simulacion("e-dirigido", "Dirigido", "PHISHING_DIRIGIDO", 90, "2002-05-20T10:00:00");
        simulacion("e-smishing", "Smishing", "SMISHING", 40, "2002-05-31T23:59:59");
        // Fuera del periodo
        simulacion(LAB, "Laboratorio", "LABORATORIO", 100, "2002-04-30T23:59:59");
        simulacion("e-dirigido", "Dirigido", "PHISHING_DIRIGIDO", 100, "2002-06-01T00:00:00");
    }

    private void progreso(long usuarioId, long leccionId, long moduloId, double porcentaje, boolean completado) {
        Progreso p = new Progreso();
        p.setUsuarioId(usuarioId);
        p.setLeccionId(leccionId);
        p.setModuloId(moduloId);
        p.setPorcentaje(porcentaje);
        p.setCompletado(completado);
        progresoRepository.save(p);
    }

    @Test
    void elAdminRecibeElPanelCompleto() throws Exception {
        when(authClient.obtenerMetricas(any(), any()))
                .thenReturn(new MetricasUsuariosFeign(12, 10, Map.of("ADMIN", 1L, "ESTUDIANTE", 11L), 2, 7, 25));
        sembrarSimulacionesDeMayo2002();
        progreso(900L, 1L, 1L, 100.0, true);
        progreso(900L, 2L, 1L, 50.0, false);
        progreso(901L, 3L, 2L, 80.0, true);

        panel("2002-05-01", "2002-05-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.desde").value("2002-05-01"))
                .andExpect(jsonPath("$.periodo.hasta").value("2002-05-31"))
                .andExpect(jsonPath("$.fuentesNoDisponibles").isEmpty())
                .andExpect(jsonPath("$.certificados").doesNotExist())
                // usuarios (auth-service)
                .andExpect(jsonPath("$.usuarios.totalUsuarios").value(12))
                .andExpect(jsonPath("$.usuarios.porRol.ESTUDIANTE").value(11))
                .andExpect(jsonPath("$.usuarios.bloqueadosAhora").value(2))
                .andExpect(jsonPath("$.usuarios.iniciosSesionEnPeriodo").value(25))
                // progreso (estado actual)
                .andExpect(jsonPath("$.progreso.promedioPorcentaje").value(76.67))
                .andExpect(jsonPath("$.progreso.leccionesCompletadas").value(2))
                .andExpect(jsonPath("$.progreso.estudiantesConProgreso").value(2))
                .andExpect(jsonPath("$.progreso.porModulo[0].moduloId").value(1))
                .andExpect(jsonPath("$.progreso.porModulo[0].promedio").value(75.0))
                .andExpect(jsonPath("$.progreso.porModulo[0].completadas").value(1))
                // simulaciones del periodo: los extremos se incluyen y lo de fuera no cuenta
                .andExpect(jsonPath("$.simulaciones.completadas").value(4))
                .andExpect(jsonPath("$.simulaciones.aprobadas").value(2))
                .andExpect(jsonPath("$.simulaciones.tasaAprobacion").value(50.0))
                .andExpect(jsonPath("$.simulaciones.porTipo.PHISHING.completadas").value(2))
                .andExpect(jsonPath("$.simulaciones.porTipo.PHISHING.aprobadas").value(1))
                .andExpect(jsonPath("$.simulaciones.porTipo.PHISHING.promedioPorcentaje").value(65.0))
                .andExpect(jsonPath("$.simulaciones.porTipo.LABORATORIO.completadas").value(2))
                .andExpect(jsonPath("$.simulaciones.porTipo.LABORATORIO.promedioPorcentaje").value(75.0))
                .andExpect(jsonPath("$.simulaciones.topEscenarios.length()").value(3))
                .andExpect(jsonPath("$.simulaciones.topEscenarios[0].escenarioUuid").value(LAB))
                .andExpect(jsonPath("$.simulaciones.topEscenarios[0].completadas").value(2))
                .andExpect(jsonPath("$.simulaciones.topEscenarios[0].tasaAprobacion").value(50.0));

        verify(authClient).obtenerMetricas(LocalDate.of(2002, 5, 1), LocalDate.of(2002, 5, 31));
    }

    @Test
    void lasSimulacionesSeFiltranPorElPeriodo() throws Exception {
        when(authClient.obtenerMetricas(any(), any()))
                .thenReturn(new MetricasUsuariosFeign(0, 0, Map.of(), 0, 0, 0));
        simulacion("e-filtro", "Filtro", "PHISHING_CORREO", 80, "2002-08-10T12:00:00");
        simulacion("e-filtro", "Filtro", "PHISHING_CORREO", 30, "2002-08-11T12:00:00");

        panel("2002-08-10", "2002-08-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulaciones.completadas").value(1))
                .andExpect(jsonPath("$.simulaciones.tasaAprobacion").value(100.0));
        panel("2002-09-01", "2002-09-30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulaciones.completadas").value(0))
                .andExpect(jsonPath("$.simulaciones.tasaAprobacion").value(0.0))
                .andExpect(jsonPath("$.simulaciones.porTipo.LABORATORIO.promedioPorcentaje").value(0.0))
                .andExpect(jsonPath("$.simulaciones.topEscenarios").isEmpty());
    }

    @Test
    void siAuthFallaElPanelSeEntregaSinUsuarios() throws Exception {
        when(authClient.obtenerMetricas(any(), any())).thenThrow(new RuntimeException("auth-service caído"));

        panel("2002-05-01", "2002-05-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarios").doesNotExist())
                .andExpect(jsonPath("$.fuentesNoDisponibles[0]").value("auth"))
                .andExpect(jsonPath("$.simulaciones").exists())
                .andExpect(jsonPath("$.progreso").exists());
    }

    @Test
    void soloElAdminPuedeVerElPanel() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", token("ESTUDIANTE"))).andExpect(status().isForbidden());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaPeriodosInvalidos() throws Exception {
        panel("2002-05-31", "2002-05-01").andExpect(status().isBadRequest());
        panel("2001-01-01", "2002-01-02").andExpect(status().isBadRequest());
        panel("2002-05-01", "no-es-fecha").andExpect(status().isBadRequest());
        // 366 días es el máximo permitido
        when(authClient.obtenerMetricas(any(), any())).thenReturn(new MetricasUsuariosFeign(0, 0, Map.of(), 0, 0, 0));
        panel("2002-01-01", "2003-01-01").andExpect(status().isOk());
    }

    @Test
    void sinFechasUsaLosUltimos30Dias() throws Exception {
        when(authClient.obtenerMetricas(any(), any())).thenReturn(new MetricasUsuariosFeign(0, 0, Map.of(), 0, 0, 0));
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);

        panel(null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.hasta").value(hoy.toString()))
                .andExpect(jsonPath("$.periodo.desde").value(hoy.minusDays(29).toString()));
    }
}
