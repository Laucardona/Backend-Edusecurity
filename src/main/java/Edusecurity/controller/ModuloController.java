package Edusecurity.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

import Edusecurity.dto.ModuloDTO;
import Edusecurity.entity.ModuloEducativo;
import Edusecurity.service.ModuloService;

@RestController
@RequestMapping("/modulos")

public class ModuloController {

    @Autowired
    private ModuloService service;

    @PostMapping
    public ModuloEducativo guardar(
            @RequestBody
            ModuloDTO dto){

        return service
                .guardar(dto);

    }

    @GetMapping
    public List<ModuloEducativo>
        listar(){

        return service
                .listar();

    }

}