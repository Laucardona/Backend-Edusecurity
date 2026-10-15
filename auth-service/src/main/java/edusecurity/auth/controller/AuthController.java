package edusecurity.auth.controller;

import edusecurity.auth.dto.AuthResponse;
import edusecurity.auth.dto.FortalezaPasswordRequest;
import edusecurity.auth.dto.FortalezaPasswordResponse;
import edusecurity.auth.dto.LogoutRequest;
import edusecurity.auth.dto.RecuperarPasswordRequest;
import edusecurity.auth.dto.RestablecerPasswordRequest;
import edusecurity.auth.dto.SesionResponse;
import edusecurity.auth.dto.LoginRequest;
import edusecurity.auth.dto.MiCuentaRequest;
import edusecurity.auth.dto.RefreshRequest;
import edusecurity.auth.dto.RegisterRequest;
import edusecurity.auth.dto.UsuarioResponse;
import edusecurity.auth.service.AuthenticationService;
import edusecurity.auth.service.ClienteInfo;
import edusecurity.auth.service.RecuperacionPasswordService;
import edusecurity.auth.service.SesionService;
import edusecurity.common.security.TokenInfo;
import edusecurity.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Autenticación", description = "Registro, login, sesiones, recuperación de contraseña y perfil")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RecuperacionPasswordService recuperacionPasswordService;
    private final SesionService sesionService;

    public AuthController(AuthenticationService authenticationService,
                          RecuperacionPasswordService recuperacionPasswordService,
                          SesionService sesionService) {
        this.authenticationService = authenticationService;
        this.recuperacionPasswordService = recuperacionPasswordService;
        this.sesionService = sesionService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un nuevo estudiante", description = "Público. Siempre crea el rol ESTUDIANTE.")
    public UsuarioResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve access token y refresh token (JWT). Tras varios intentos fallidos "
                    + "la cuenta se bloquea temporalmente (HTTP 423).")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authenticationService.login(request, ClienteInfo.from(http));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el access token", description = "Intercambia un refresh token válido por uno nuevo (rotación).")
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authenticationService.refrescar(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesión",
            description = "Invalida el refresh token enviado. Con todas=true cierra todas las sesiones del usuario.")
    public void logout(@AuthenticationPrincipal TokenInfo principal,
                       @RequestBody(required = false) LogoutRequest request) {
        authenticationService.logout(principal, request);
    }

    @GetMapping("/sesiones")
    @Operation(summary = "Historial de inicios de sesión del usuario autenticado",
            description = "Fecha, hora, IP y dispositivo de cada sesión, de la más reciente a la más antigua.")
    public PageResponse<SesionResponse> misSesiones(@AuthenticationPrincipal TokenInfo principal,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return sesionService.historial(principal.usuarioId(), pageable);
    }

    @PostMapping("/password/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar recuperación de contraseña",
            description = "Público. Envía un enlace al correo si la cuenta existe; la respuesta es la misma en ambos casos.")
    public void recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest request) {
        recuperacionPasswordService.solicitar(request.correo());
    }

    @PostMapping("/password/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablecer la contraseña con el token recibido por correo",
            description = "Público. El token es de un solo uso; al cambiarla se cierran todas las sesiones.")
    public void restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        recuperacionPasswordService.restablecer(request.token(), request.passwordNuevo());
    }

    @PostMapping("/password/fortaleza")
    @Operation(summary = "Evaluar la fortaleza de una contraseña",
            description = "Público. Indica si cumple la política y qué le falta, para el indicador del frontend.")
    public FortalezaPasswordResponse fortaleza(@Valid @RequestBody FortalezaPasswordRequest request) {
        return authenticationService.evaluarPassword(request.password());
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
