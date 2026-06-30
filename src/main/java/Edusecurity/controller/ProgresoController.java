package Edusecurity.controller;

import Edusecurity.entity.ProgresoUsuario;
import Edusecurity.service.ProgresoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progreso")
@CrossOrigin("*")
public class ProgresoController {

    private final ProgresoService progresoService;

    public ProgresoController(ProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    @GetMapping
    public List<ProgresoUsuario> obtenerTodos() {
        return progresoService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public ProgresoUsuario obtenerPorId(@PathVariable Long id) {
        return progresoService.obtenerPorId(id);
    }

    @PostMapping
    public ProgresoUsuario crear(@RequestBody ProgresoUsuario progreso) {
        return progresoService.guardar(progreso);
    }

    @PutMapping("/{id}")
    public ProgresoUsuario actualizar(@PathVariable Long id,
                                      @RequestBody ProgresoUsuario progreso) {
        progreso.setId(id);
        return progresoService.guardar(progreso);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        progresoService.eliminar(id);
    }
}
