package Edusecurity.controller;

import Edusecurity.dto.ProgresoDTO;
import Edusecurity.entity.ProgresoUsuario;
import Edusecurity.service.ProgresoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progreso")
@CrossOrigin("*")
public class ProgresoController {

    private final ProgresoService service;

    public ProgresoController(ProgresoService service) {
        this.service = service;
    }

    @PostMapping
    public ProgresoUsuario guardar(
            @RequestBody ProgresoDTO dto) {

        return service.guardar(dto);

    }

    @GetMapping
    public List<ProgresoUsuario> listar() {

        return service.listar();

    }

    @GetMapping("/{id}")
    public ProgresoUsuario obtener(
            @PathVariable Long id) {

        return service.obtenerPorId(id);

    }

    @GetMapping("/usuario/{id}")
    public List<ProgresoUsuario> buscarUsuario(
            @PathVariable Long id) {

        return service.buscarPorUsuario(id);

    }

    @DeleteMapping("/{id}")
    public void eliminar(
            @PathVariable Long id) {

        service.eliminar(id);

    }

}