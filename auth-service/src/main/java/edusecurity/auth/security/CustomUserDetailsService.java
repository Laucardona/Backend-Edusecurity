package edusecurity.auth.security;

import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga los usuarios del sistema para el AuthenticationManager de Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String rol = (usuario.getRol() == null) ? "" : usuario.getRol().getNombre();

        return org.springframework.security.core.userdetails.User
                .withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities("ROLE_" + rol)
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}
