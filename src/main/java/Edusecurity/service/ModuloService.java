package Edusecurity.service;

import Edusecurity.dto.ModuloDTO;
import Edusecurity.entity.ModuloEducativo;
import Edusecurity.repository.ModuloRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuloService {

    private final ModuloRepository repository;

    public ModuloService(ModuloRepository repository) {
        this.repository = repository;
    }

    public ModuloEducativo guardar(ModuloDTO dto) {

        ModuloEducativo modulo = new ModuloEducativo();

        modulo.setTitulo(dto.getTitulo());
        modulo.setDescripcion(dto.getDescripcion());
        modulo.setActivo(true);

        return repository.save(modulo);
    }

    public List<ModuloEducativo> listar() {
        return repository.findAll();
    }

    public ModuloEducativo obtenerPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public ModuloEducativo actualizar(Long id, ModuloDTO dto) {

        ModuloEducativo modulo = repository.findById(id).orElse(null);

        if (modulo == null) {
            return null;
        }

        modulo.setTitulo(dto.getTitulo());
        modulo.setDescripcion(dto.getDescripcion());

        return repository.save(modulo);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

}