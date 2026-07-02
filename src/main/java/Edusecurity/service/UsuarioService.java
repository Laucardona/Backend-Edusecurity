package Edusecurity.service;

import Edusecurity.dto.UsuarioDTO;
import Edusecurity.entity.Rol;
import Edusecurity.entity.Usuario;
import Edusecurity.repository.RolRepository;
import Edusecurity.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrar(UsuarioDTO dto) {

        if (usuarioRepository.findByCorreo(dto.getCorreo()).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setActivo(true);


        Rol rol;
        if (dto.getRolId() != null) {
            rol = rolRepository.findById(dto.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        } else {
            rol = rolRepository.findById(2L) // ajusta el ID del rol por defecto en tu BD
                    .orElseThrow(() -> new RuntimeException("Rol por defecto no configurado"));
        }
        usuario.setRol(rol);

        return usuarioRepository.save(usuario);
    }
}