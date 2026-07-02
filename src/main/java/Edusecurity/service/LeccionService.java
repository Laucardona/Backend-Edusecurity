package Edusecurity.service;

import Edusecurity.dto.LeccionDTO;
import Edusecurity.entity.Leccion;
import Edusecurity.entity.ModuloEducativo;
import Edusecurity.repository.LeccionRepository;
import Edusecurity.repository.ModuloRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeccionService {

    private final LeccionRepository repository;
    private final ModuloRepository moduloRepository;

    public LeccionService(LeccionRepository repository,
                          ModuloRepository moduloRepository) {

        this.repository = repository;
        this.moduloRepository = moduloRepository;
    }

    public Leccion guardar(LeccionDTO dto) {

        ModuloEducativo modulo =
                moduloRepository.findById(dto.getModuloId()).orElse(null);

        if (modulo == null) {
            return null;
        }

        Leccion leccion = new Leccion();

        leccion.setTitulo(dto.getTitulo());
        leccion.setContenido(dto.getContenido());
        leccion.setModulo(modulo);

        return repository.save(leccion);
    }

    public List<Leccion> listar() {
        return repository.findAll();
    }

    public Leccion obtenerPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Leccion> buscarPorModulo(Long moduloId) {
        return repository.findByModuloId(moduloId);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
    

}