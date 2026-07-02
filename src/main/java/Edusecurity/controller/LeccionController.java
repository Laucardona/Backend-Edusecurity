package Edusecurity.controller;

import Edusecurity.dto.LeccionDTO;
import Edusecurity.entity.Leccion;
import Edusecurity.service.LeccionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lecciones")
@CrossOrigin("*")
public class LeccionController {

    private final LeccionService service;

    public LeccionController(LeccionService service) {
        this.service = service;
    }

    @PostMapping
    public Leccion guardar(
            @RequestBody LeccionDTO dto) {

        return service.guardar(dto);

    }

    @GetMapping
    public List<Leccion> listar() {

        return service.listar();

    }

    @GetMapping("/{id}")
    public Leccion obtener(
            @PathVariable Long id) {

        return service.obtenerPorId(id);

    }

    @GetMapping("/modulo/{id}")
    public List<Leccion> buscarPorModulo(
            @PathVariable Long id) {

        return service.buscarPorModulo(id);

    }

    @DeleteMapping("/{id}")
    public void eliminar(
            @PathVariable Long id) {

        service.eliminar(id);

    }

}