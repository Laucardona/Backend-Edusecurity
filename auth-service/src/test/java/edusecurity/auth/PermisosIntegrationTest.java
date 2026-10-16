package edusecurity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.repository.RolRepository;
import edusecurity.common.security.JwtSupport;
import edusecurity.common.security.Permisos;
import edusecurity.common.security.TokenInfo;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Permisos por rol (tarea 1.2): seed, claim del token y autorización por permiso. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermisosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private RolRepository rolRepository;

    @Test
    void elSeedDeLaBaseCoincideConElCatalogoDelCodigo() {
        Map<String, Set<String>> enBase = rolRepository.findAllByOrderByNombreAsc().stream()
                .filter(r -> Permisos.PREDETERMINADOS.containsKey(r.getNombre()))
                .collect(Collectors.toMap(Rol::getNombre, r -> new TreeSet<>(r.codigosPermisos())));

        assertThat(enBase).isEqualTo(Permisos.PREDETERMINADOS);
    }

    @Test
    void elAccessTokenDelLoginLlevaLosPermisosDelRol() throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@edusecurity.com\",\"password\":\"Admin123*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TokenInfo info = jwtSupport.parseAccessToken(objectMapper.readTree(json).get("accessToken").asText());

        assertThat(info.rol()).isEqualTo("ADMIN");
        assertThat(info.permisos()).containsExactlyElementsOf(Permisos.PREDETERMINADOS.get("ADMIN"));
    }

    @Test
    void sinElPermisoUsuariosAdministrarNoSeListanUsuarios() throws Exception {
        for (String rol : new String[] {"ESTUDIANTE", "GESTOR_CONTENIDO", "SOPORTE"}) {
            mockMvc.perform(get("/api/usuarios").header("Authorization",
                            "Bearer " + jwtSupport.generateAccessToken("x@x.com", 900L, rol)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/roles").header("Authorization",
                        "Bearer " + jwtSupport.generateAccessToken("g@x.com", 901L, "GESTOR_CONTENIDO")))
                .andExpect(status().isForbidden());
    }
}
