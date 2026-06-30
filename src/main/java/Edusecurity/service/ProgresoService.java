package Edusecurity.service;

import Edusecurity.entity.ProgresoUsuario;
import Edusecurity.repository.ProgresoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProgresoService {

    private final ProgresoRepository progresoRepository;

    public ProgresoService(ProgresoRepository progresoRepository) {
        this.progresoRepository = progresoRepository;
    }

    public List<ProgresoUsuario> obtenerTodos() {
        return progresoRepository.findAll();
    }

    public ProgresoUsuario obtenerPorId(Long id) {
        return progresoRepository.findById(id).orElse(null);
    }

    public ProgresoUsuario guardar(ProgresoUsuario progreso) {
        return progresoRepository.save(progreso);
    }

    public void eliminar(Long id) {
        progresoRepository.deleteById(id);
    }
}