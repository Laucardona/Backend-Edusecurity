package edusecurity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edusecurity.auth.repository.RolRepository;
import edusecurity.common.security.JwtSupport;
import edusecurity.common.security.Permisos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Roles con permisos, desbloqueo y sesiones por soporte (tarea 1.3). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RolesYSoporteIntegrationTest {

    private static final String PASSWORD = "Segura123*";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtSupport jwtSupport;

    @Autowired
    private RolRepository rolRepository;

    private String bearer(String rol) {
        return "Bearer " + jwtSupport.generateAccessToken(rol.toLowerCase() + "@test.com", 999L, rol);
    }

    private Long registrar(String correo) throws Exception {
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Soporte\",\"correo\":\"" + correo + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private ResultActions login(String correo, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions cambiarPermisos(Long rolId, String rol, String... permisos) throws Exception {
        return mockMvc.perform(put("/api/roles/" + rolId + "/permisos").header("Authorization", bearer(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("permisos", permisos))));
    }

    // ------------------------------------------------------------------ roles

    @Test
    void listarRolesIncluyeSusPermisos() throws Exception {
        mockMvc.perform(get("/api/roles").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'ADMIN')].permisos[*]").value(hasItem(Permisos.ROLES_ADMINISTRAR)))
                .andExpect(jsonPath("$[?(@.nombre == 'SOPORTE')].permisos[*]").value(hasItem(Permisos.USUARIOS_DESBLOQUEAR)));
    }

    @Test
    void cambiarLosPermisosDeUnRolNuevo() throws Exception {
        String json = mockMvc.perform(post("/api/roles").header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"auditor_prueba\",\"descripcion\":\"Solo auditoría\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        cambiarPermisos(id, "ADMIN", Permisos.AUDITORIA_VER, Permisos.PERFIL_GESTIONAR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("AUDITOR_PRUEBA"))
                .andExpect(jsonPath("$.permisos.length()").value(2))
                .andExpect(jsonPath("$.permisos[0]").value(Permisos.AUDITORIA_VER));

        cambiarPermisos(id, "ADMIN", "no:existe").andExpect(status().isBadRequest());
        cambiarPermisos(id, "GESTOR_CONTENIDO", Permisos.AUDITORIA_VER).andExpect(status().isForbidden());
    }

    @Test
    void elRolAdminNoPuedePerderRolesAdministrar() throws Exception {
        Long adminId = rolRepository.findByNombre("ADMIN").orElseThrow().getId();

        cambiarPermisos(adminId, "ADMIN", Permisos.PERFIL_GESTIONAR, Permisos.USUARIOS_ADMINISTRAR)
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/roles/" + adminId).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisos").value(hasItem(Permisos.ROLES_ADMINISTRAR)))
                .andExpect(jsonPath("$.permisos.length()").value(Permisos.PREDETERMINADOS.get("ADMIN").size()));
    }

    // ------------------------------------------------------------------ soporte

    @Test
    void soportePuedeDesbloquearPeroNoListarUsuarios() throws Exception {
        String correo = "bloqueada-soporte@correo.com";
        Long id = registrar(correo);
        for (int i = 0; i < 5; i++) {
            login(correo, "Incorrecta1*");
        }
        login(correo, PASSWORD).andExpect(status().isLocked());

        mockMvc.perform(post("/api/usuarios/" + id + "/desbloquear").header("Authorization", bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer("SOPORTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/usuarios/" + id + "/desbloquear").header("Authorization", bearer("SOPORTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueadoHasta").doesNotExist());
        login(correo, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void soporteVeLasSesionesDeUnUsuarioSinElToken() throws Exception {
        String correo = "sesiones-soporte@correo.com";
        Long id = registrar(correo);
        JsonNode tokens = objectMapper.readTree(login(correo, PASSWORD).andReturn().getResponse().getContentAsString());
        String refresh = tokens.get("refreshToken").asText();

        String json = mockMvc.perform(get("/api/usuarios/" + id + "/sesiones").header("Authorization", bearer("SOPORTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].activa").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain(refresh).doesNotContainIgnoringCase("hash").doesNotContain("refreshToken");

        mockMvc.perform(get("/api/usuarios/" + id + "/sesiones").header("Authorization", bearer("GESTOR_CONTENIDO")))
                .andExpect(status().isForbidden());
    }
}
