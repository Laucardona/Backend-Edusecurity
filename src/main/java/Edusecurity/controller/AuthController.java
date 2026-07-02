package Edusecurity.controller;

import Edusecurity.dto.LoginRequest;
import Edusecurity.dto.LoginResponse;
import Edusecurity.dto.UsuarioDTO;
import Edusecurity.entity.Usuario;
import Edusecurity.service.AuthService;
import Edusecurity.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public AuthController(UsuarioService usuarioService,
                          AuthService authService) {

        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public Usuario registrar(@RequestBody UsuarioDTO dto) {

        return usuarioService.registrar(dto);

    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request) {

        return authService.login(request);

    }

}