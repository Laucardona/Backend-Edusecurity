package edusecurity.auth.config;

import edusecurity.auth.entity.Rol;
import edusecurity.auth.entity.Usuario;
import edusecurity.auth.repository.RolRepository;
import edusecurity.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales: roles ADMIN/ESTUDIANTE y un usuario administrador por defecto
 * (admin@edusecurity.com / Admin123*). Idempotente: solo inserta si no existen.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    public static final String ADMIN_CORREO = "admin@edusecurity.com";
    public static final String ADMIN_PASSWORD = "Admin123*";

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearRol("ADMIN", "Administrador del sistema");
        crearRol("ESTUDIANTE", "Estudiante de la plataforma");

        if (!usuarioRepository.existsByCorreo(ADMIN_CORREO)) {
            Rol admin = rolRepository.findByNombre("ADMIN").orElseThrow();

            Usuario usuario = new Usuario();
            usuario.setNombre("Administrador");
            usuario.setCorreo(ADMIN_CORREO);
            usuario.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            usuario.setActivo(true);
            usuario.setRol(admin);
            usuarioRepository.save(usuario);

            log.warn("Usuario administrador creado: {}. Cambie la contraseña inicial.", ADMIN_CORREO);
        }
    }

    private void crearRol(String nombre, String descripcion) {
        if (!rolRepository.existsByNombre(nombre)) {
            rolRepository.save(new Rol(nombre, descripcion));
            log.info("Rol creado: {}", nombre);
        }
    }
}
