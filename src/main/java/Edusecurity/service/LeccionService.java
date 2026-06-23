package Edusecurity.service;

import Edusecurity.entity.Leccion;
import Edusecurity.repository.LeccionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeccionService {

    private final LeccionRepository leccionRepository;

    public LeccionService(LeccionRepository leccionRepository) {
        this.leccionRepository = leccionRepository;
    }

    public List<Leccion> obtenerTodas() {
        return leccionRepository.findAll();
    }

    public Leccion obtenerPorId(Long id) {
        return leccionRepository.findById(id).orElse(null);
    }

    public Leccion guardar(Leccion leccion) {
        return leccionRepository.save(leccion);
    }

    public void eliminar(Long id) {
        leccionRepository.deleteById(id);
    }
}
