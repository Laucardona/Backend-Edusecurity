package edusecurity.auth.controller;

import edusecurity.auth.dto.AuthResponse;
import edusecurity.auth.dto.LoginRequest;
import edusecurity.auth.dto.MiCuentaRequest;
import edusecurity.auth.dto.RefreshRequest;
import edusecurity.auth.dto.RegisterRequest;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.service.AuthenticationService;
import edusecurity.common.security.TokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, login, refresh token y perfil del usuario")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un nuevo estudiante", description = "Público. Siempre crea el rol ESTUDIANTE.")
    public UsuarioResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve access token y refresh token (JWT).")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el access token", description = "Intercambia un refresh token válido por uno nuevo (rotación).")
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authenticationService.refrescar(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    public UsuarioResponse miCuenta(@AuthenticationPrincipal TokenInfo principal) {
        return authenticationService.obtenerMiCuenta(principal);
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar el perfil del usuario autenticado",
            description = "Para cambiar la contraseña se debe enviar passwordActual.")
    public UsuarioResponse actualizarMiCuenta(@AuthenticationPrincipal TokenInfo principal,
                                              @Valid @RequestBody MiCuentaRequest request) {
        return authenticationService.actualizarMiCuenta(principal, request);
    }
}
