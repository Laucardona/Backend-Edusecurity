package Edusecurity.controller;

import Edusecurity.entity.Leccion;
import Edusecurity.service.LeccionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lecciones")
@CrossOrigin("*")
public class LeccionController {

    private final LeccionService leccionService;

    public LeccionController(LeccionService leccionService) {
        this.leccionService = leccionService;
    }

    @GetMapping
    public List<Leccion> obtenerTodas() {
        return leccionService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public Leccion obtenerPorId(@PathVariable Long id) {
        return leccionService.obtenerPorId(id);
    }

    @PostMapping
    public Leccion crear(@RequestBody Leccion leccion) {
        return leccionService.guardar(leccion);
    }

    @PutMapping("/{id}")
    public Leccion actualizar(@PathVariable Long id,
                              @RequestBody Leccion leccion) {
        leccion.setId(id);
        return leccionService.guardar(leccion);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        leccionService.eliminar(id);
    }
}
    

