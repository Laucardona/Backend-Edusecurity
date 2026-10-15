package edusecurity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Sesion;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.SesionRepository;
import edusecurity.auth.repository.UsuarioRepository;
import edusecurity.common.security.JwtSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Métricas internas para el panel administrativo (HU33). Otras clases de prueba comparten la base,
 * así que los totales de usuarios se comparan como diferencia y las sesiones se siembran en 2001,
 * un periodo que ninguna otra prueba usa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricasUsuariosIntegrationTest {

    private static final String URL = "/internal/usuarios/metricas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private SesionRepository sesionRepository;

    @Value("${app.security.internal-key}")
    private String claveInterna;

    private JsonNode metricas(String desde, String hasta) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(URL).param("desde", desde).param("hasta", hasta)
                        .header("X-Service-Key", claveInterna))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private Usuario usuario(String rol, boolean activo, Instant bloqueadoHasta) {
        Usuario u = new Usuario();
        u.setNombre("Métricas");
        u.setCorreo("metricas-" + UUID.randomUUID() + "@correo.com");
        u.setPassword("$2a$10$hashDePruebaNoUsadoParaIniciarSesion..............");
        u.setActivo(activo);
        u.setRol(rol == null ? null : rolRepository.findByNombre(rol).orElseThrow());
        u.setBloqueadoHasta(bloqueadoHasta);
        return usuarioRepository.save(u);
    }

    private void sesion(Usuario u, Instant creadaEn) {
        Sesion s = new Sesion();
        s.setUsuario(u);
        s.setRefreshTokenHash(UUID.randomUUID().toString().replace("-", "") + "00000000000000000000000000000000".substring(0, 32));
        s.setCreadaEn(creadaEn);
        s.setUltimoUso(creadaEn);
        s.setExpiraEn(creadaEn.plus(7, ChronoUnit.DAYS));
        sesionRepository.save(s);
    }

    @Test
    void conLaClaveInternaDevuelveConteosCorrectos() throws Exception {
        JsonNode antes = metricas("2001-03-01", "2001-03-31");

        Usuario estudiante1 = usuario("ESTUDIANTE", true, null);
        Usuario estudiante2 = usuario("ESTUDIANTE", true, Instant.now().plus(10, ChronoUnit.MINUTES));
        usuario("ESTUDIANTE", false, Instant.now().minus(10, ChronoUnit.MINUTES)); // bloqueo ya vencido
        Usuario admin = usuario("ADMIN", true, null);

        // Dentro del periodo: 3 inicios de sesión de 2 usuarios. Fuera: 2 (antes y el día siguiente).
        sesion(estudiante1, Instant.parse("2001-03-01T00:00:00Z"));
        sesion(estudiante1, Instant.parse("2001-03-15T12:00:00Z"));
        sesion(admin, Instant.parse("2001-03-31T23:59:59Z"));
        sesion(estudiante2, Instant.parse("2001-02-28T23:59:59Z"));
        sesion(estudiante2, Instant.parse("2001-04-01T00:00:00Z"));

        JsonNode despues = metricas("2001-03-01", "2001-03-31");

        assertThat(despues.get("totalUsuarios").asLong() - antes.get("totalUsuarios").asLong()).isEqualTo(4);
        assertThat(despues.get("usuariosActivos").asLong() - antes.get("usuariosActivos").asLong()).isEqualTo(3);
        assertThat(despues.get("bloqueadosAhora").asLong() - antes.get("bloqueadosAhora").asLong()).isEqualTo(1);
        assertThat(despues.get("porRol").get("ESTUDIANTE").asLong() - antes.get("porRol").path("ESTUDIANTE").asLong())
                .isEqualTo(3);
        assertThat(despues.get("porRol").get("ADMIN").asLong() - antes.get("porRol").get("ADMIN").asLong()).isEqualTo(1);
        assertThat(despues.get("iniciosSesionEnPeriodo").asLong()).isEqualTo(3);
        assertThat(despues.get("usuariosConSesionEnPeriodo").asLong()).isEqualTo(2);

        // Un día que no incluye ninguna sesión sembrada.
        JsonNode vacio = metricas("2001-03-02", "2001-03-14");
        assertThat(vacio.get("iniciosSesionEnPeriodo").asLong()).isZero();
        assertThat(vacio.get("usuariosConSesionEnPeriodo").asLong()).isZero();
    }

    @Test
    void unEstudianteRecibe403YSinAutenticacion401() throws Exception {
        String token = "Bearer " + jwtSupport.generateAccessToken("e@correo.com", 5L, "ESTUDIANTE");
        mockMvc.perform(get(URL).param("desde", "2001-03-01").param("hasta", "2001-03-31").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL).param("desde", "2001-03-01").param("hasta", "2001-03-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaUnPeriodoInvertido() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2001-03-31").param("hasta", "2001-03-01")
                        .header("X-Service-Key", claveInterna))
                .andExpect(status().isBadRequest());
    }
}
