package edusecurity.auth.config;

import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales: roles ADMIN/ESTUDIANTE y un usuario administrador. El correo y la contraseña
 * del administrador llegan por las variables ADMIN_CORREO y ADMIN_PASSWORD; sin ADMIN_PASSWORD no
 * se crea ningún administrador. Idempotente: solo inserta si no existen.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminCorreo;
    private final String adminPassword;

    public DataInitializer(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.correo:}") String adminCorreo,
                           @Value("${app.admin.password:}") String adminPassword) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminCorreo = adminCorreo == null ? "" : adminCorreo.trim();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @Override
    public void run(String... args) {
        crearRol("ADMIN", "Administrador del sistema");
        crearRol("ESTUDIANTE", "Estudiante de la plataforma");
        crearAdministrador();
    }

    private void crearAdministrador() {
        if (adminCorreo.isEmpty() || adminPassword.isEmpty()) {
            log.warn("ADMIN_CORREO o ADMIN_PASSWORD están vacíos: no se crea el usuario administrador inicial.");
            return;
        }
        if (usuarioRepository.existsByCorreo(adminCorreo)) {
            return;
        }
        Rol admin = rolRepository.findByNombre("ADMIN").orElseThrow();

        Usuario usuario = new Usuario();
        usuario.setNombre("Administrador");
        usuario.setCorreo(adminCorreo);
        usuario.setPassword(passwordEncoder.encode(adminPassword));
        usuario.setActivo(true);
        usuario.setRol(admin);
        usuarioRepository.save(usuario);

        // Nunca se registra la contraseña.
        log.warn("Usuario administrador inicial creado: {}. Cambie la contraseña tras el primer ingreso.", adminCorreo);
    }

    private void crearRol(String nombre, String descripcion) {
        if (!rolRepository.existsByNombre(nombre)) {
            rolRepository.save(new Rol(nombre, descripcion));
            log.info("Rol creado: {}", nombre);
        }
    }
}
