package edusecurity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edusecurity.auth.config.DataInitializer;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Administrador inicial desde variables de entorno (tarea 0.8). */
@ExtendWith(OutputCaptureExtension.class)
class DataInitializerTest {

    private final RolRepository rolRepository = mock(RolRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void sinContrasenaNoCreaAdministradorYLoAdvierte(CapturedOutput salida) {
        new DataInitializer(rolRepository, usuarioRepository, passwordEncoder, "admin@x.com", "").run();

        verify(usuarioRepository, never()).save(any());
        assertThat(salida.getOut()).contains("no se crea el usuario administrador");
    }

    @Test
    void conContrasenaCreaElAdministradorSinRegistrarLaContrasena(CapturedOutput salida) {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.of(new Rol("ADMIN", "Administrador")));
        when(passwordEncoder.encode("Clave-Inicial-2026")).thenReturn("hash");

        new DataInitializer(rolRepository, usuarioRepository, passwordEncoder, "jefa@x.com", "Clave-Inicial-2026").run();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCorreo()).isEqualTo("jefa@x.com");
        assertThat(guardado.getValue().getPassword()).isEqualTo("hash");
        assertThat(salida.getOut()).contains("jefa@x.com").doesNotContain("Clave-Inicial-2026");
    }
}
