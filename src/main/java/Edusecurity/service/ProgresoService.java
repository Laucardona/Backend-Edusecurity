package Edusecurity.service;

import Edusecurity.dto.ProgresoDTO;
import Edusecurity.entity.Leccion;
import Edusecurity.entity.ProgresoUsuario;
import Edusecurity.entity.Usuario;
import Edusecurity.repository.LeccionRepository;
import Edusecurity.repository.ProgresoRepository;
import Edusecurity.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProgresoService {

    private final ProgresoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final LeccionRepository leccionRepository;

    public ProgresoService(ProgresoRepository repository,
                           UsuarioRepository usuarioRepository,
                           LeccionRepository leccionRepository) {

        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.leccionRepository = leccionRepository;
    }

    public ProgresoUsuario guardar(ProgresoDTO dto) {

        Usuario usuario =
                usuarioRepository.findById(dto.getUsuarioId()).orElse(null);

        Leccion leccion =
                leccionRepository.findById(dto.getLeccionId()).orElse(null);

        if (usuario == null || leccion == null) {
            return null;
        }

        ProgresoUsuario progreso = new ProgresoUsuario();

        progreso.setUsuario(usuario);
        progreso.setLeccion(leccion);
        progreso.setPorcentajeCompletado(dto.getPorcentajeCompletado());
        progreso.setCompletado(dto.getCompletado());

        return repository.save(progreso);
    }

    public List<ProgresoUsuario> listar() {
        return repository.findAll();
    }

    public ProgresoUsuario obtenerPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<ProgresoUsuario> buscarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

}