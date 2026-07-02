package Edusecurity.controller;

import Edusecurity.dto.ModuloDTO;
import Edusecurity.entity.ModuloEducativo;
import Edusecurity.service.ModuloService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modulos")
@CrossOrigin("*")
public class ModuloController {

    private final ModuloService service;

    public ModuloController(ModuloService service) {
        this.service = service;
    }

    @PostMapping
    public ModuloEducativo guardar(
            @RequestBody ModuloDTO dto) {

        return service.guardar(dto);

    }

    @GetMapping
    public List<ModuloEducativo> listar() {

        return service.listar();

    }

    @GetMapping("/{id}")
    public ModuloEducativo obtener(
            @PathVariable Long id) {

        return service.obtenerPorId(id);

    }

    @PutMapping("/{id}")
    public ModuloEducativo actualizar(
            @PathVariable Long id,
            @RequestBody ModuloDTO dto) {

        return service.actualizar(id, dto);

    }

    @DeleteMapping("/{id}")
    public void eliminar(
            @PathVariable Long id) {

        service.eliminar(id);

    }

}