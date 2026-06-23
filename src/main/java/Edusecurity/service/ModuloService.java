package Edusecurity.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Edusecurity.dto.ModuloDTO;
import Edusecurity.entity.ModuloEducativo;
import Edusecurity.repository.ModuloRepository;

@Service
public class ModuloService {

    @Autowired
    private ModuloRepository repository;

    public ModuloEducativo guardar(
            ModuloDTO dto){

        ModuloEducativo modulo =
                new ModuloEducativo();

        modulo.setTitulo(
                dto.getTitulo());

        modulo.setDescripcion(
                dto.getDescripcion());

        modulo.setActivo(true);

        return repository.save(
                modulo);

    }

    public List<ModuloEducativo>
        listar(){

        return repository.findAll();

    }

}